package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterCommand;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterUseCase;
import com.hd.gamematch.friendlymatch.application.port.out.LoadHostRosterEditContextPort;
import com.hd.gamematch.friendlymatch.domain.HostRosterEditContext;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterLockedException;
import com.hd.gamematch.friendlymatch.application.port.out.SaveHostRosterPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfirmHostRosterService implements ConfirmHostRosterUseCase {

    private final LoadHostRosterEditContextPort loadHostRosterEditContextPort;
    private final SaveHostRosterPort saveHostRosterPort;
    private final FindGameUserUseCase findGameUserUseCase;

    // 실제 경기 주최자를 확인한 뒤 호스트 팀원 명단을 저장한다.
    @Override
    public FriendlyMatch confirm(ConfirmHostRosterCommand command) {
        // Optional.empty()는 조회 결과 없음이므로 경기 미존재 예외로 변환한다.
        HostRosterEditContext context = loadHostRosterEditContextPort.loadForUpdate(command.matchId())
                .orElseThrow(FriendlyMatchNotFoundException::new);
        FriendlyMatch match = context.match();

        // 인증 사용자가 실제 주최자가 아니면 저장 전에 요청을 거부한다.
        if (!match.host().userId().equals(command.userId())) {
            throw new HostRosterAccessDeniedException();
        }

        // 정원이 남은 개인전도 첫 승인 기록이 있으면 주최 명단을 바꿀 수 없다.
        if (context.hasApprovedRequests()) {
            throw new HostRosterLockedException();
        }

        // 권한 확인 후 저장 전에 실제 참가자 명단을 검증한다.
        validateRoster(command.teamMemberIds(), match.capacity(), match.game().id());

        // 조회한 경기 ID와 전달받은 명단으로 저장하고 저장 결과를 반환한다.
        return saveHostRosterPort.saveHostRoster(match.id(), command.teamMemberIds());
    }

    // 실제 참가자 수와 중복 및 경기 게임 일치를 확인한다.
    private void validateRoster(List<Long> memberIds, int capacity, Long gameId) {
        // TEAM은 팀별 한도, INDIVIDUAL은 경기 전체 한도이며 주최자를 자동 합산하지 않는다.
        if (memberIds.size() > capacity) {
            throw new IllegalArgumentException("참가자 명단이 최대 인원을 초과합니다.");
        }
        if (new HashSet<>(memberIds).size() != memberIds.size()) {
            throw new IllegalArgumentException("참가자 명단에 중복된 게임 프로필이 있습니다.");
        }

        // 빈 명단은 조회 없이 허용하고 나머지는 한 번의 일괄 조회로 존재 여부를 확인한다.
        if (memberIds.isEmpty()) {
            return;
        }
        List<GameUserProfile> members = findGameUserUseCase.findGameUsers(memberIds);
        for (GameUserProfile member : members) {
            if (!member.game().id().equals(gameId)) {
                throw new IllegalArgumentException("참가자 게임 프로필의 게임과 경기 게임이 일치하지 않습니다.");
            }
        }
    }
}

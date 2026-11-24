package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterCommand;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterUseCase;
import com.hd.gamematch.friendlymatch.application.port.out.LoadFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveHostRosterPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfirmHostRosterService implements ConfirmHostRosterUseCase {

    private final LoadFriendlyMatchPort loadFriendlyMatchPort;
    private final SaveHostRosterPort saveHostRosterPort;

    // 실제 경기 주최자를 확인한 뒤 호스트 팀원 명단을 저장한다.
    @Override
    public FriendlyMatch confirm(ConfirmHostRosterCommand command) {
        // Optional.empty()는 조회 결과 없음이므로 경기 미존재 예외로 변환한다.
        FriendlyMatch match = loadFriendlyMatchPort.loadFriendlyMatch(command.matchId())
                .orElseThrow(FriendlyMatchNotFoundException::new);

        // 인증 사용자가 실제 주최자가 아니면 저장 전에 요청을 거부한다.
        if (!match.host().userId().equals(command.userId())) {
            throw new HostRosterAccessDeniedException();
        }

        // 조회한 경기 ID와 전달받은 명단으로 저장하고 저장 결과를 반환한다.
        return saveHostRosterPort.saveHostRoster(match.id(), command.teamMemberIds());
    }
}

package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchNotRecruitingException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAlreadyActiveException;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestPort;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestTargetPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveMatchRequestPort;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.friendlymatch.domain.MatchRequestTarget;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateMatchRequestService implements CreateMatchRequestUseCase {

    private final FindGameUserUseCase findGameUserUseCase;
    private final LoadMatchRequestTargetPort loadMatchRequestTargetPort;
    private final LoadMatchRequestPort loadMatchRequestPort;
    private final SaveMatchRequestPort saveMatchRequestPort;

    // 신청 프로필과 팀원을 검증하고 신규 신청 또는 기존 신청의 재활성화를 저장한다.
    @Override
    public CreateMatchRequestResult createRequest(CreateMatchRequestCommand command) {
        GameUserProfile requester = findGameUserUseCase.findGameUser(
                FindGameUserQuery.of(command.gameUserId()));
        // 인증 사용자는 신청자 프로필의 소유자여야 한다.
        if (!requester.userId().equals(command.userId())) {
            throw new MatchRequestAccessDeniedException();
        }

        MatchRequestTarget target = loadMatchRequestTargetPort.loadMatchRequestTarget(command.matchId())
                .orElseThrow(FriendlyMatchNotFoundException::new);
        if (!target.recruiting()) {
            throw new MatchNotRecruitingException();
        }
        validateGame(requester, target.gameId());

        // 생략한 팀원 목록은 개인 신청을 뜻하므로 빈 목록으로 정규화한다.
        List<Long> teamMemberIds = command.teamMemberIds() == null ? List.of() : command.teamMemberIds();
        List<FindGameUserQuery> teamMemberQueries = teamMemberIds.stream()
                .map(FindGameUserQuery::of)
                .toList();
        validateUniqueProfiles(command.gameUserId(), teamMemberQueries);
        for (FindGameUserQuery query : teamMemberQueries) {
            // 대표자는 다른 사용자의 프로필도 팀원으로 포함할 수 있다.
            validateGame(findGameUserUseCase.findGameUser(query), target.gameId());
        }

        MatchRequest existing = loadMatchRequestPort.loadByMatchIdAndGameUserId(
                command.matchId(), command.gameUserId()).orElse(null);
        if (existing != null && (existing.state() == MatchRequestState.PENDING
                || existing.state() == MatchRequestState.APPROVED)) {
            throw new MatchRequestAlreadyActiveException();
        }

        // 재신청은 기존 식별자를 유지하고 현재 검증한 명단과 메시지로 교체한다.
        MatchRequest request = new MatchRequest(
                existing == null ? null : existing.id(),
                command.matchId(),
                command.gameUserId(),
                List.copyOf(teamMemberIds),
                command.comment(),
                MatchRequestState.PENDING);
        Long savedId = saveMatchRequestPort.save(request);
        return existing == null
                ? CreateMatchRequestResult.created(savedId)
                : CreateMatchRequestResult.reactivated(existing.id());
    }

    // 신청자와 모든 팀원은 대상 경기와 같은 게임의 프로필이어야 한다.
    private void validateGame(GameUserProfile profile, Long gameId) {
        if (!profile.game().id().equals(gameId)) {
            throw new IllegalArgumentException("참가 프로필의 게임과 대상 경기의 게임이 일치하지 않습니다.");
        }
    }

    // 신청자를 포함한 전체 참가 프로필의 중복을 거부한다.
    private void validateUniqueProfiles(Long requesterId, List<FindGameUserQuery> teamMemberQueries) {
        Set<Long> profileIds = new HashSet<>();
        profileIds.add(requesterId);
        for (FindGameUserQuery query : teamMemberQueries) {
            if (!profileIds.add(query.gameUserId())) {
                throw new IllegalArgumentException("참가 프로필은 중복될 수 없습니다.");
            }
        }
    }
}

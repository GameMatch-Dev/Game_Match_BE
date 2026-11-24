package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchApprovalAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.MatchApprovalConflictException;
import com.hd.gamematch.friendlymatch.application.exception.MatchNotRecruitingException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestNotFoundException;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchApprovalContextPort;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestForApprovalPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveMatchRequestApprovalPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.friendlymatch.domain.MatchApprovalContext;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ApproveMatchRequestService implements ApproveMatchRequestUseCase {

    private final LoadMatchRequestForApprovalPort loadMatchRequestForApprovalPort;
    private final LoadMatchApprovalContextPort loadMatchApprovalContextPort;
    private final SaveMatchRequestApprovalPort saveMatchRequestApprovalPort;
    private final FindGameUserUseCase findGameUserUseCase;

    // 경기 잠금 이후의 신청을 검증하고 참가자 등록과 승인 및 모집 상태 변경을 요청한다.
    @Override
    public ApproveMatchRequestResult approve(ApproveMatchRequestCommand command) {
        Long matchId = loadMatchRequestForApprovalPort.findMatchIdByRequestId(command.requestId())
                .orElseThrow(MatchRequestNotFoundException::new);

        // 같은 경기의 변경을 잠금 안으로 모으고 최신 신청 상태를 다시 읽는다.
        MatchApprovalContext context = loadMatchApprovalContextPort.loadForUpdate(matchId)
                .orElseThrow(FriendlyMatchNotFoundException::new);
        MatchRequest request = loadMatchRequestForApprovalPort.loadRequestById(command.requestId())
                .orElseThrow(MatchRequestNotFoundException::new);

        // 재승인 성공을 돌려주기 전에도 주최자 권한부터 확인한다.
        if (!context.hostUserId().equals(command.userId())) {
            throw new MatchApprovalAccessDeniedException();
        }
        List<Long> participantIds = participantIds(request);
        if (request.state() == MatchRequestState.APPROVED) {
            return approvalResult(request, participantIds);
        }

        validateApprovalState(context, request);
        validateCapacity(context, participantIds.size());
        validateUniqueParticipants(context, participantIds);
        validateParticipantGames(context.gameId(), participantIds);

        // 저장 구현은 이 트랜잭션 안에서 신청과 참가자 및 모집 상태를 함께 변경해야 한다.
        boolean closeRecruitment = context.sort() == FriendlyMatchSort.TEAM
                || totalParticipantCount(context, participantIds.size()) == context.capacity();
        saveMatchRequestApprovalPort.saveApproval(matchId, request.id(), participantIds, closeRecruitment);
        return approvalResult(request, participantIds);
    }

    // 신청 대표와 추가 팀원 전체를 실제 승인 참가자 목록으로 만든다.
    private List<Long> participantIds(MatchRequest request) {
        List<Long> ids = new ArrayList<>();
        ids.add(request.gameUserId());
        ids.addAll(request.teamMemberIds());
        return List.copyOf(ids);
    }

    // 대기 상태와 모집 상태 및 명단 확정 여부를 확인한다.
    private void validateApprovalState(MatchApprovalContext context, MatchRequest request) {
        if (request.state() != MatchRequestState.PENDING) {
            throw new MatchApprovalConflictException("대기 중인 참가 신청만 승인할 수 있습니다.");
        }
        if (context.state() != FriendlyMatchState.RECRUITING) {
            throw new MatchNotRecruitingException();
        }
        if (!context.hostRosterConfirmed()) {
            throw new MatchApprovalConflictException("주최 측 참가 명단이 확정되지 않았습니다.");
        }
        if (context.sort() == FriendlyMatchSort.TEAM && context.opponentTeamApproved()) {
            throw new MatchApprovalConflictException("이미 상대 팀의 참가 신청이 승인되었습니다.");
        }
    }

    // TEAM은 각 팀의 상한을 검사하고 개인전은 전체 실제 참가자를 합산한다.
    private void validateCapacity(MatchApprovalContext context, int newParticipantCount) {
        boolean exceeded = context.sort() == FriendlyMatchSort.TEAM
                ? context.hostTeamMemberIds().size() > context.capacity()
                    || newParticipantCount > context.capacity()
                : totalParticipantCount(context, newParticipantCount) > context.capacity();
        if (exceeded) {
            throw new MatchApprovalConflictException("참가 신청을 승인하면 최대 인원을 초과합니다.");
        }
    }

    // 주최 측과 기존 승인 참가자를 포함한 전체 실제 인원을 계산한다.
    private long totalParticipantCount(MatchApprovalContext context, int newParticipantCount) {
        return (long) context.hostTeamMemberIds().size()
                + context.approvedParticipantIds().size() + newParticipantCount;
    }

    // 기존 참가자와의 중복 및 새 신청 명단 내부의 중복을 함께 거부한다.
    private void validateUniqueParticipants(MatchApprovalContext context, List<Long> participantIds) {
        Set<Long> registeredIds = new HashSet<>(context.hostTeamMemberIds());
        registeredIds.addAll(context.approvedParticipantIds());
        for (Long participantId : participantIds) {
            if (!registeredIds.add(participantId)) {
                throw new MatchApprovalConflictException("경기 참가자는 중복될 수 없습니다.");
            }
        }
    }

    // 일괄 조회로 프로필 존재 여부를 확인하고 경기 게임과의 일치를 검증한다.
    private void validateParticipantGames(Long gameId, List<Long> participantIds) {
        for (GameUserProfile profile : findGameUserUseCase.findGameUsers(participantIds)) {
            if (!profile.game().id().equals(gameId)) {
                throw new IllegalArgumentException("참가 프로필의 게임과 대상 경기의 게임이 일치하지 않습니다.");
            }
        }
    }

    // 새 승인과 저장 없는 재승인에 동일한 응답 결과를 사용한다.
    private ApproveMatchRequestResult approvalResult(MatchRequest request, List<Long> participantIds) {
        return ApproveMatchRequestResult.of(request.id(), request.matchId(),
                MatchRequestState.APPROVED, participantIds);
    }
}

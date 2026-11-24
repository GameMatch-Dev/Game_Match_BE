package com.hd.gamematch.friendlymatch.domain;

import java.util.List;

// 경기 잠금 후 승인 여부와 실제 참가 정원을 판단할 현재 정보를 모은다.
public record MatchApprovalContext(
        Long matchId,
        Long hostUserId,
        Long gameId,
        FriendlyMatchSort sort,
        FriendlyMatchState state,
        int capacity,
        boolean hostRosterConfirmed,
        List<Long> hostTeamMemberIds,
        List<Long> approvedParticipantIds,
        boolean opponentTeamApproved
) {

    // 조회 결과의 명단을 외부에서 수정하지 못하도록 복사한다.
    public MatchApprovalContext {
        hostTeamMemberIds = List.copyOf(hostTeamMemberIds);
        approvedParticipantIds = List.copyOf(approvedParticipantIds);
    }
}

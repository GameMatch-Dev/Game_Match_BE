package com.hd.gamematch.friendlymatch.application.port.in.approve;

import com.hd.gamematch.friendlymatch.domain.MatchRequestState;

import java.util.List;

// 승인된 신청과 경기 및 해당 신청의 실제 참가자 정보를 전달한다.
public record ApproveMatchRequestResult(
        Long requestId,
        Long matchId,
        MatchRequestState status,
        List<Long> participantGameUserIds
) {

    // 승인 결과의 참가자 목록을 외부에서 변경하지 못하도록 복사한다.
    public ApproveMatchRequestResult {
        participantGameUserIds = List.copyOf(participantGameUserIds);
    }

    // 업무 처리 결과를 승인 응답에 필요한 정보로 묶는다.
    public static ApproveMatchRequestResult of(
            Long requestId,
            Long matchId,
            MatchRequestState status,
            List<Long> participantGameUserIds
    ) {
        return new ApproveMatchRequestResult(requestId, matchId, status, participantGameUserIds);
    }
}

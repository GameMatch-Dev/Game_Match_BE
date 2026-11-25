package com.hd.gamematch.friendlymatch.adapter.in.web.response;

import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestResult;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;

import java.util.List;

public record ApproveMatchRequestResponse(
        Long requestId,
        Long matchId,
        MatchRequestState status,
        List<Long> participantGameUserIds
) {

    // 업무 결과에서 이번 승인 요청의 공개 응답 데이터를 만든다.
    public static ApproveMatchRequestResponse from(ApproveMatchRequestResult result) {
        return new ApproveMatchRequestResponse(
                result.requestId(), result.matchId(), result.status(), result.participantGameUserIds());
    }
}

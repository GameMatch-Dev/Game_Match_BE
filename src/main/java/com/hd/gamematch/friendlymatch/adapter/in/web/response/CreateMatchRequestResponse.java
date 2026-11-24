package com.hd.gamematch.friendlymatch.adapter.in.web.response;

import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestResult;

public record CreateMatchRequestResponse(Long requestId) {

    // 참가 신청 결과에서 공개 응답에 필요한 신청 ID만 추출한다.
    public static CreateMatchRequestResponse from(CreateMatchRequestResult result) {
        return new CreateMatchRequestResponse(result.requestId());
    }
}

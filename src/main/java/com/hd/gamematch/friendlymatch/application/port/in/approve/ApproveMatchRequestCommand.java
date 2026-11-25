package com.hd.gamematch.friendlymatch.application.port.in.approve;

// 참가 신청 승인에 필요한 인증 사용자와 신청 ID를 전달한다.
public record ApproveMatchRequestCommand(Long userId, Long requestId) {

    // 인증 정보와 경로의 신청 ID를 승인 입력값으로 묶는다.
    public static ApproveMatchRequestCommand of(Long userId, Long requestId) {
        return new ApproveMatchRequestCommand(userId, requestId);
    }
}

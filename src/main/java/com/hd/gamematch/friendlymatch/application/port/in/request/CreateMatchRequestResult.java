package com.hd.gamematch.friendlymatch.application.port.in.request;

// 참가 신청 ID와 신규 생성 여부를 함께 전달한다.
public record CreateMatchRequestResult(
        Long requestId,
        boolean created
) {

    // 새로 생성된 참가 신청 ID와 신규 생성 여부를 반환한다.
    public static CreateMatchRequestResult created(Long requestId) {
        return new CreateMatchRequestResult(requestId, true);
    }

    // 재활성화된 기존 참가 신청 ID와 신규 생성이 아님을 반환한다.
    public static CreateMatchRequestResult reactivated(Long requestId) {
        return new CreateMatchRequestResult(requestId, false);
    }
}

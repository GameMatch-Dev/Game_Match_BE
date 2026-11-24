package com.hd.gamematch.friendlymatch.domain;

// 참가 신청의 내부 처리 상태를 구분한다.
public enum MatchRequestState {
    // 승인 대기 중인 신청이다.
    PENDING,
    // 승인된 신청이다.
    APPROVED,
    // 취소된 신청이다.
    CANCELLED,
    // 거절된 신청이다.
    REJECTED
}

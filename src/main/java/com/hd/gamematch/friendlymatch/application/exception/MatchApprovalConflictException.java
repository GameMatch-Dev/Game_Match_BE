package com.hd.gamematch.friendlymatch.application.exception;

public class MatchApprovalConflictException extends RuntimeException {

    // 현재 신청 상태와 명단 또는 정원으로 승인할 수 없는 이유를 전달한다.
    public MatchApprovalConflictException(String message) {
        super(message);
    }
}

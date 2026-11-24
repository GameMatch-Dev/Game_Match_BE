package com.hd.gamematch.friendlymatch.application.exception;

public class MatchApprovalAccessDeniedException extends RuntimeException {

    // 경기 주최자가 아닌 사용자의 참가 신청 승인을 거부한다.
    public MatchApprovalAccessDeniedException() {
        super("참가 신청을 승인할 권한이 없습니다.");
    }
}

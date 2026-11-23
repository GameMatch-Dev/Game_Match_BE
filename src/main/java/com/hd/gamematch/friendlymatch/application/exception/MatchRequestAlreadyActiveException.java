package com.hd.gamematch.friendlymatch.application.exception;

public class MatchRequestAlreadyActiveException extends RuntimeException {

    // 참가 신청 처리 중 발생한 업무 오류의 원인을 전달한다.
    public MatchRequestAlreadyActiveException() {
        super("이미 활성 상태인 참가 신청이 있습니다.");
    }
}

package com.hd.gamematch.friendlymatch.application.exception;

public class MatchRequestAccessDeniedException extends RuntimeException {

    // 참가 신청 처리 중 발생한 업무 오류의 원인을 전달한다.
    public MatchRequestAccessDeniedException() {
        super("참가 신청에 사용할 게임 프로필의 소유자가 아닙니다.");
    }
}

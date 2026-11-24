package com.hd.gamematch.friendlymatch.application.exception;

public class MatchNotRecruitingException extends RuntimeException {

    // 참가 신청 처리 중 발생한 업무 오류의 원인을 전달한다.
    public MatchNotRecruitingException() {
        super("모집 중인 친선 경기가 아닙니다.");
    }
}

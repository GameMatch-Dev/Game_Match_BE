package com.hd.gamematch.friendlymatch.application.exception;

public class MatchRequestNotFoundException extends RuntimeException {

    // 승인 대상 신청의 조회 결과가 없을 때 사용한다.
    public MatchRequestNotFoundException() {
        super("참가 신청을 찾을 수 없습니다.");
    }
}

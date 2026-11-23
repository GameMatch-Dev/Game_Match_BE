package com.hd.gamematch.friendlymatch.application.exception;

public class FriendlyMatchNotFoundException extends RuntimeException {

    // 참가 신청 처리 중 발생한 업무 오류의 원인을 전달한다.
    public FriendlyMatchNotFoundException() {
        super("친선 경기를 찾을 수 없습니다.");
    }
}

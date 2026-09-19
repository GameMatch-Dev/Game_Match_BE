package com.hd.gamematch.friendlymatch.application.exception;

// 다른 사용자의 게임 프로필을 호스트로 친선 경기 모집을 만들 때 발생한다.
public class FriendlyMatchAccessDeniedException extends RuntimeException {

    public FriendlyMatchAccessDeniedException() {
        super("친선 경기 모집을 만들 권한이 없습니다.");
    }
}

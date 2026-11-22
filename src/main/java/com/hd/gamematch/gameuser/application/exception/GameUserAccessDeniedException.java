package com.hd.gamematch.gameuser.application.exception;

public class GameUserAccessDeniedException extends RuntimeException {

    public GameUserAccessDeniedException() {
        super("게임 프로필을 수정할 권한이 없습니다.");
    }
}

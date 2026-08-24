package com.hd.gamematch.gameuser.application.exception;

/**
 * 유효한 식별자로 게임 프로필을 조회했지만 대상이 없을 때 발생한다.
 */
public class GameUserNotFoundException extends RuntimeException {

    public GameUserNotFoundException() {
        super("게임 프로필을 찾을 수 없습니다.");
    }
}

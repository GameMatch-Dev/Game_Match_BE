package com.hd.gamematch.gameuser.application.exception;

/**
 * 한 사용자가 같은 게임에 게임 프로필을 두 번 등록하려 할 때 발생한다.
 */
public class GameUserAlreadyRegisteredException extends RuntimeException {

    public GameUserAlreadyRegisteredException() {
        super("이미 해당 게임에 게임 프로필이 등록되어 있습니다.");
    }

    public GameUserAlreadyRegisteredException(Throwable cause) {
        super("이미 해당 게임에 게임 프로필이 등록되어 있습니다.", cause);
    }
}
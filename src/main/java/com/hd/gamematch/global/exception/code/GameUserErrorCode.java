package com.hd.gamematch.global.exception.code;

import com.hd.gamematch.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GameUserErrorCode implements ErrorCode {
    GAME_USER_001(
            HttpStatus.NOT_FOUND,
            "GAME_USER_001",
            "게임 프로필을 찾을 수 없습니다."
    ),
    GAME_USER_002(
            HttpStatus.BAD_REQUEST,
            "GAME_USER_002",
            "이미 해당 게임에서 사용 중인 닉네임입니다."
    ),
    GAME_USER_003(
            HttpStatus.CONFLICT,
            "GAME_USER_003",
            "이미 해당 게임에 게임 프로필이 등록되어 있습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    GameUserErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}

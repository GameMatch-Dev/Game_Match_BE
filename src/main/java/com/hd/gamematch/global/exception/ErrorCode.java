package com.hd.gamematch.global.exception;

import org.springframework.http.HttpStatus;

// 오류 응답에 사용하는 HTTP 상태와 코드 문자열을 한곳에서 관리한다.
public enum ErrorCode {
    COMMON_400(HttpStatus.BAD_REQUEST, "COMMON_400"),
    GAME_001(HttpStatus.NOT_FOUND, "GAME_001");

    private final HttpStatus status;
    private final String code;

    ErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}

package com.hd.gamematch.global.exception;

import org.springframework.http.HttpStatus;

// 오류 응답에 사용하는 HTTP 상태와 코드 문자열을 한곳에서 관리한다.
public enum ErrorCode {
    COMMON_400(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    GAME_001(HttpStatus.NOT_FOUND, "GAME_001", "게임을 찾을 수 없습니다."),
    AUTH_400(HttpStatus.BAD_REQUEST, "AUTH_400", "로그인 요청이 올바르지 않습니다."),
    AUTH_401(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증 정보가 없거나 유효하지 않습니다."),
    AUTH_PROVIDER_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AUTH_PROVIDER_UNAVAILABLE",
            "카카오 로그인 서비스를 일시적으로 사용할 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}

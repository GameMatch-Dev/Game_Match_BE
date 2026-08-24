package com.hd.gamematch.global.exception.code;

import com.hd.gamematch.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
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

    AuthErrorCode(HttpStatus status, String code, String defaultMessage) {
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

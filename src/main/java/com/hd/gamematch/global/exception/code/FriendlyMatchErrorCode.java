package com.hd.gamematch.global.exception.code;

import com.hd.gamematch.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum FriendlyMatchErrorCode implements ErrorCode {
    FRIENDLY_MATCH_001(HttpStatus.NOT_FOUND, "FRIENDLY_MATCH_001", "친선 경기를 찾을 수 없습니다."),
    FRIENDLY_MATCH_002(HttpStatus.CONFLICT, "FRIENDLY_MATCH_002", "참가 신청 승인 후에는 주최 측 명단을 변경할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    FriendlyMatchErrorCode(HttpStatus status, String code, String defaultMessage) {
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

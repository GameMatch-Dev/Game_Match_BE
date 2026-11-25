package com.hd.gamematch.global.exception.code;

import com.hd.gamematch.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MatchRequestErrorCode implements ErrorCode {
    MATCH_REQUEST_001(HttpStatus.NOT_FOUND, "MATCH_REQUEST_001", "참가 신청을 찾을 수 없습니다."),
    MATCH_REQUEST_002(HttpStatus.CONFLICT, "MATCH_REQUEST_002", "이미 활성 상태인 참가 신청이 있습니다."),
    MATCH_REQUEST_003(HttpStatus.CONFLICT, "MATCH_REQUEST_003", "모집 중인 친선 경기가 아닙니다."),
    MATCH_REQUEST_004(HttpStatus.CONFLICT, "MATCH_REQUEST_004", "참가 신청을 승인할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    MatchRequestErrorCode(HttpStatus status, String code, String defaultMessage) {
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

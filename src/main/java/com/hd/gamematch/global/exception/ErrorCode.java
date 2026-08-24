package com.hd.gamematch.global.exception;

import org.springframework.http.HttpStatus;

// 모든 도메인 오류 코드가 공통 HTTP 오류 응답으로 변환되기 위해 구현하는 계약이다.
public interface ErrorCode {

    HttpStatus status();

    String code();

    String defaultMessage();
}

package com.hd.gamematch.auth.application.exception;

/** POST /auth/token에 필요한 ticket 형식이 없을 때만 사용하는 인증 입력 예외다. */
public class InvalidAuthRequestException extends RuntimeException {

    public InvalidAuthRequestException() {
        super("로그인 티켓이 필요합니다.");
    }
}

package com.hd.gamematch.auth.application.exception;

public class InvalidLoginTicketException extends IllegalArgumentException {

    public InvalidLoginTicketException() {
        super("로그인 티켓이 만료되었거나 이미 사용되었습니다. 다시 로그인해 주세요.");
    }
}

package com.hd.gamematch.auth.adapter.in.web.exception;

import com.hd.gamematch.auth.application.exception.InvalidAuthRequestException;
import com.hd.gamematch.auth.application.exception.InvalidLoginTicketException;
import com.hd.gamematch.global.exception.ErrorCode;
import com.hd.gamematch.global.exception.code.AuthErrorCode;
import com.hd.gamematch.global.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 인증 웹 어댑터가 만든 업무 예외를 인증 API 계약의 HTTP 응답으로 변환한다.
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidAuthRequestException.class)
    public ResponseEntity<CommonResponse<Void>> handleInvalidAuthRequest(InvalidAuthRequestException exception) {
        return errorResponse(AuthErrorCode.AUTH_400);
    }

    @ExceptionHandler(InvalidLoginTicketException.class)
    public ResponseEntity<CommonResponse<Void>> handleInvalidLoginTicket(InvalidLoginTicketException exception) {
        return errorResponse(AuthErrorCode.AUTH_401);
    }

    private ResponseEntity<CommonResponse<Void>> errorResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status())
                .body(new CommonResponse<>(false, errorCode.code(), errorCode.defaultMessage(), null));
    }
}

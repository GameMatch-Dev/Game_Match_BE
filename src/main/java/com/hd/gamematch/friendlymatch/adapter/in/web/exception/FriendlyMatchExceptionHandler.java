package com.hd.gamematch.friendlymatch.adapter.in.web.exception;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchAccessDeniedException;
import com.hd.gamematch.global.exception.code.AuthErrorCode;
import com.hd.gamematch.global.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 친선 경기 웹 어댑터의 업무 예외를 HTTP 오류 응답으로 변환한다.
@RestControllerAdvice
public class FriendlyMatchExceptionHandler {

    // 타인의 게임 프로필을 호스트로 지정한 요청을 403 응답으로 변환한다.
    @ExceptionHandler(FriendlyMatchAccessDeniedException.class)
    public ResponseEntity<CommonResponse<Void>> handleFriendlyMatchAccessDenied(
            FriendlyMatchAccessDeniedException exception
    ) {
        return ResponseEntity.status(AuthErrorCode.AUTH_403.status())
                .body(new CommonResponse<>(
                        false,
                        AuthErrorCode.AUTH_403.code(),
                        exception.getMessage(),
                        null
                ));
    }
}

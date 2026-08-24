package com.hd.gamematch.gameuser.adapter.in.web.exception;

import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.global.exception.ErrorCode;
import com.hd.gamematch.global.exception.code.GameUserErrorCode;
import com.hd.gamematch.global.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 게임 프로필 웹 어댑터가 만든 업무 예외를 게임 프로필 API 계약의 HTTP 응답으로 변환한다.
@RestControllerAdvice
public class GameUserExceptionHandler {

    @ExceptionHandler(GameUserNotFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleGameUserNotFound(
            GameUserNotFoundException exception
    ) {
        return errorResponse(GameUserErrorCode.GAME_USER_001);
    }

    @ExceptionHandler(GameUserAlreadyRegisteredException.class)
    public ResponseEntity<CommonResponse<Void>> handleGameUserAlreadyRegistered(
            GameUserAlreadyRegisteredException exception
    ) {
        return errorResponse(GameUserErrorCode.GAME_USER_003);
    }

    @ExceptionHandler(GameUserNicknameAlreadyInUseException.class)
    public ResponseEntity<CommonResponse<Void>> handleGameUserNicknameAlreadyInUse(
            GameUserNicknameAlreadyInUseException exception
    ) {
        return errorResponse(GameUserErrorCode.GAME_USER_002);
    }

    private ResponseEntity<CommonResponse<Void>> errorResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status())
                .body(new CommonResponse<>(false, errorCode.code(), errorCode.defaultMessage(), null));
    }
}

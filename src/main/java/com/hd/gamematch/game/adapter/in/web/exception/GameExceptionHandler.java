package com.hd.gamematch.game.adapter.in.web.exception;

import com.hd.gamematch.game.application.exception.GameNotFoundException;
import com.hd.gamematch.global.exception.code.GameErrorCode;
import com.hd.gamematch.global.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 게임 조회 과정에서 발생한 업무 예외를 게임 API 계약의 HTTP 응답으로 변환한다.
@RestControllerAdvice
public class GameExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleGameNotFound(GameNotFoundException exception) {
        return ResponseEntity.status(GameErrorCode.GAME_001.status())
                .body(new CommonResponse<>(false, GameErrorCode.GAME_001.code(), exception.getMessage(), null));
    }
}

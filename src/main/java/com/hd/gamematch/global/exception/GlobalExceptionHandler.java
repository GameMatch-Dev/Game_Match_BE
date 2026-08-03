package com.hd.gamematch.global.exception;

import com.hd.gamematch.game.application.exception.GameNotFoundException;
import com.hd.gamematch.global.response.CommonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 모든 컨트롤러에서 올라온 예외를 한곳에서 HTTP 응답으로 바꾸는 전역 처리기다.
// @RestControllerAdvice는 처리 결과를 화면이 아닌 JSON 본문으로 작성하게 한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스가 GameNotFoundException을 던졌을 때만 이 메서드를 골라 실행한다.
    // 그래서 컨트롤러마다 try-catch를 반복하지 않고도 같은 404 규칙을 적용할 수 있다.
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleGameNotFound(GameNotFoundException exception) {
        // 업무 예외를 클라이언트와 약속한 HTTP 404 + 공통 JSON 응답으로 변환하는 경계다.
        // Void와 null은 이 실패 응답에는 돌려줄 게임 데이터가 없다는 뜻이다.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CommonResponse<>(false, "GAME_NOT_FOUND", exception.getMessage(), null));
    }
}

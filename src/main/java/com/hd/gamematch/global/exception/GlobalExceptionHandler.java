package com.hd.gamematch.global.exception;

import com.hd.gamematch.global.exception.code.CommonErrorCode;
import com.hd.gamematch.global.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

// 모든 컨트롤러에서 올라온 예외를 한곳에서 HTTP 응답으로 바꾸는 전역 처리기다.
// @RestControllerAdvice는 처리 결과를 화면이 아닌 JSON 본문으로 작성하게 한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity.status(CommonErrorCode.COMMON_400.status())
                .body(new CommonResponse<>(
                        false,
                        CommonErrorCode.COMMON_400.code(),
                        CommonErrorCode.COMMON_400.defaultMessage(),
                        null
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        return ResponseEntity.status(CommonErrorCode.COMMON_400.status())
                .body(new CommonResponse<>(
                        false,
                        CommonErrorCode.COMMON_400.code(),
                        CommonErrorCode.COMMON_400.defaultMessage(),
                        null
                ));
    }

    // FindGameQuery 같은 입력 검증 코드에서 IllegalArgumentException이 발생하면 이 메서드가 처리한다.
    // 예외에 담긴 구체적인 이유는 유지하고, HTTP 상태와 공통 오류 코드는 400 규칙으로 통일한다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.status(CommonErrorCode.COMMON_400.status())
                .body(new CommonResponse<>(false, CommonErrorCode.COMMON_400.code(), exception.getMessage(), null));
    }

}

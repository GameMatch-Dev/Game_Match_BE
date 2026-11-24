package com.hd.gamematch.friendlymatch.adapter.in.web.exception;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchNotRecruitingException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAlreadyActiveException;
import com.hd.gamematch.global.exception.ErrorCode;
import com.hd.gamematch.global.exception.code.AuthErrorCode;
import com.hd.gamematch.global.exception.code.FriendlyMatchErrorCode;
import com.hd.gamematch.global.exception.code.MatchRequestErrorCode;
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

    // 존재하지 않는 친선 경기의 신청을 404 응답으로 변환한다.
    @ExceptionHandler(FriendlyMatchNotFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleFriendlyMatchNotFound(
            FriendlyMatchNotFoundException exception
    ) {
        return errorResponse(FriendlyMatchErrorCode.FRIENDLY_MATCH_001, exception.getMessage());
    }

    // 신청자 게임 프로필의 소유권 오류를 403 응답으로 변환한다.
    @ExceptionHandler(MatchRequestAccessDeniedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMatchRequestAccessDenied(
            MatchRequestAccessDeniedException exception
    ) {
        return errorResponse(AuthErrorCode.AUTH_403, exception.getMessage());
    }

    // 이미 활성 상태인 중복 신청을 409 응답으로 변환한다.
    @ExceptionHandler(MatchRequestAlreadyActiveException.class)
    public ResponseEntity<CommonResponse<Void>> handleMatchRequestAlreadyActive(
            MatchRequestAlreadyActiveException exception
    ) {
        return errorResponse(MatchRequestErrorCode.MATCH_REQUEST_002, exception.getMessage());
    }

    // 모집이 끝난 경기의 참가 신청을 409 응답으로 변환한다.
    @ExceptionHandler(MatchNotRecruitingException.class)
    public ResponseEntity<CommonResponse<Void>> handleMatchNotRecruiting(
            MatchNotRecruitingException exception
    ) {
        return errorResponse(MatchRequestErrorCode.MATCH_REQUEST_003, exception.getMessage());
    }

    // 업무 예외의 메시지와 약속된 오류 코드를 공통 응답에 담는다.
    private ResponseEntity<CommonResponse<Void>> errorResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.status())
                .body(new CommonResponse<>(false, errorCode.code(), message, null));
    }
}

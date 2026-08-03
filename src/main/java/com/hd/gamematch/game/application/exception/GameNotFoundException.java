package com.hd.gamematch.game.application.exception;

// "게임을 찾지 못함"을 다른 입력 오류와 구별하기 위한 업무 전용 예외다.
// RuntimeException을 상속하므로 메서드마다 throws를 적지 않아도 위쪽 계층까지 전달된다.
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException() {
        // 응답 메시지를 예외 안에 한 번만 정해 서비스와 API 응답이 같은 문구를 사용하게 한다.
        // 테스트는 이 문구가 바뀌지 않았는지 검증해 API 계약을 지킨다.
        super("게임을 찾을 수 없습니다.");
    }
}

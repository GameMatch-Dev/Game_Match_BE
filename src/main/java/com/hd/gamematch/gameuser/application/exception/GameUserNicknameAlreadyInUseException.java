package com.hd.gamematch.gameuser.application.exception;

/**
 * 같은 게임에서 이미 사용 중인 닉네임으로
 * 게임 프로필 등록을 시도했을 때 발생한다.
 */
public class GameUserNicknameAlreadyInUseException extends RuntimeException {

    public GameUserNicknameAlreadyInUseException() {
        // 부모 클래스인 RuntimeException의 생성자를 호출하는 코드
        // 예외 객체 안에 메시지를 저장
        super("이미 해당 게임에서 사용 중인 닉네임입니다.");
    }
}
package com.hd.gamematch.gameuser.application.port.in.register;

/**
 * 게임 프로필 등록 기능에 전달할 입력값이다.
 *
 * <p>HTTP 요청값(gameId, nickname)과 인증 주체(userId)를 하나로 묶는다.</p>
 */
public record RegisterGameUserCommand(
        Long userId,
        Long gameId,
        String nickname
) {
}

package com.hd.gamematch.gameuser.application.port.in;

/**
 * 게임 프로필 단건 조회에 필요한 입력값과 최소 유효성 규칙을 묶는다.
 */
public record FindGameUserQuery(Long gameUserId) {

    public FindGameUserQuery {
        if (gameUserId == null) {
            throw new IllegalArgumentException("gameUserId는 필수입니다.");
        }
        if (gameUserId <= 0) {
            throw new IllegalArgumentException("gameUserId는 1 이상이어야 합니다.");
        }
    }

    public static FindGameUserQuery of(Long gameUserId) {
        return new FindGameUserQuery(gameUserId);
    }
}

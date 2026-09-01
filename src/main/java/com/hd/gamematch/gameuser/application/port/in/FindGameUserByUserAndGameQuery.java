package com.hd.gamematch.gameuser.application.port.in;

/**
 * 사용자와 게임으로 게임 프로필을 조회하는 데 필요한 입력값을 묶는다.
 */
public record FindGameUserByUserAndGameQuery(Long userId, Long gameId) {

    public FindGameUserByUserAndGameQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId는 1 이상이어야 합니다.");
        }
        if (gameId == null || gameId <= 0) {
            throw new IllegalArgumentException("gameId는 1 이상이어야 합니다.");
        }
    }

    public static FindGameUserByUserAndGameQuery of(Long userId, Long gameId) {
        return new FindGameUserByUserAndGameQuery(userId, gameId);
    }
}

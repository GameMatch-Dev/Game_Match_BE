package com.hd.gamematch.gameuser.domain;

import com.hd.gamematch.game.domain.Game;

import java.util.Objects;

/**
 * 공개 단건 조회에 필요한 게임 프로필과 연결 게임 정보를 표현하는 읽기 모델이다.
 */
public record GameUserProfile(Long id, String nickname, Game game, Long userId) {

    public GameUserProfile {
        Objects.requireNonNull(id, "gameUserId는 필수입니다.");
        Objects.requireNonNull(nickname, "nickname은 필수입니다.");
        Objects.requireNonNull(game, "game은 필수입니다.");
        Objects.requireNonNull(userId, "userId는 필수입니다.");
    }
}

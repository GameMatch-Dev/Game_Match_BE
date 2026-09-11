package com.hd.gamematch.friendlymatch.domain;

import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.domain.GameUserProfile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 한 게임 프로필이 주최하는 친선 경기 모집의 현재 정보를 표현한다.
 */
public record FriendlyMatch(
        Long id,
        GameUserProfile host,
        Game game,
        LocalDate date,
        LocalTime time,
        FriendlyMatchSort sort,
        FriendlyMatchState state,
        Integer recruit,
        String comment
) {

    public FriendlyMatch {
        Objects.requireNonNull(id, "friendlyMatchId는 필수입니다.");
        Objects.requireNonNull(host, "host는 필수입니다.");
        Objects.requireNonNull(game, "game은 필수입니다.");
        Objects.requireNonNull(date, "date는 필수입니다.");
        Objects.requireNonNull(time, "time은 필수입니다.");
        Objects.requireNonNull(sort, "sort는 필수입니다.");
        Objects.requireNonNull(state, "state는 필수입니다.");
        Objects.requireNonNull(recruit, "recruit는 필수입니다.");
        Objects.requireNonNull(comment, "comment는 필수입니다.");
    }
}

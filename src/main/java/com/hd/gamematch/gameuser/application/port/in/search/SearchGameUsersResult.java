package com.hd.gamematch.gameuser.application.port.in.search;

import com.hd.gamematch.gameuser.domain.GameUserProfile;

import java.util.List;
import java.util.Objects;

public record SearchGameUsersResult(
        long totalCount,
        List<GameUserProfile> gameUsers
) {

    public SearchGameUsersResult {
        if (totalCount < 0) {
            throw new IllegalArgumentException("totalCount는 0 이상이어야 합니다.");
        }

        Objects.requireNonNull(gameUsers, "gameUsers는 필수입니다.");
        gameUsers = List.copyOf(gameUsers);
    }
}

package com.hd.gamematch.gameuser.application.port.in.search;

public record SearchGameUsersQuery(
        String nickname,
        Long gameId,
        int page,
        int size
) {

    public SearchGameUsersQuery {
        if (nickname == null) {
            throw new IllegalArgumentException("nickname은 필수입니다.");
        }

        nickname = nickname.trim();

        if (nickname.length() < 2) {
            throw new IllegalArgumentException("nickname은 2글자 이상이어야 합니다.");
        }

        if (gameId != null && gameId <= 0) {
            throw new IllegalArgumentException("gameId는 1 이상이어야 합니다.");
        }

        if (page < 1) {
            throw new IllegalArgumentException("page는 1 이상이어야 합니다.");
        }

        if (size < 1 || size > 20) {
            throw new IllegalArgumentException("size는 1 이상 20 이하여야 합니다.");
        }
    }

    public static SearchGameUsersQuery of(
            String nickname,
            Long gameId,
            int page,
            int size
    ) {
        return new SearchGameUsersQuery(nickname, gameId, page, size);
    }
}

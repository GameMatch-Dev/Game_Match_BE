package com.hd.gamematch.gameuser.adapter.in.web.response;

import com.hd.gamematch.game.adapter.in.web.response.GameResponse;
import com.hd.gamematch.game.domain.Game;

// 게임 프로필 등록 API가 프런트에 돌려줄 JSON 모양
public record RegisterGameUserResponse(
        Long id,
        String nickname,
        GameResponse game,
        UserResponse user
) {

    public static RegisterGameUserResponse of(
            Long id,
            String nickname,
            Game game,
            Long userId
    ) {
        return new RegisterGameUserResponse(
                id,
                nickname,
                GameResponse.from(game),
                new UserResponse(userId)
        );
    }

    public record UserResponse(
            Long id
    ) {
    }
}

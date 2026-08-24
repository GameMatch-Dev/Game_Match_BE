package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.game.adapter.in.web.GameResponse;
import com.hd.gamematch.gameuser.domain.GameUserProfile;

/**
 * 공개 게임 프로필 단건 조회의 응답 형식이다.
 */
public record FindGameUserResponse(
        Long id,
        String nickname,
        GameResponse game,
        UserResponse user
) {

    public static FindGameUserResponse from(GameUserProfile profile) {
        return new FindGameUserResponse(
                profile.id(),
                profile.nickname(),
                GameResponse.from(profile.game()),
                new UserResponse(profile.userId())
        );
    }

    public record UserResponse(Long id) {
    }
}

package com.hd.gamematch.gameuser.application.port.out;

import com.hd.gamematch.gameuser.domain.GameUserProfile;

import java.util.List;

public interface LoadGameUsersPort {

    List<GameUserProfile> loadGameUsersByNicknamePrefix(
            String nickname,
            Long gameId,
            int page,
            int size
    );

    long countGameUsersByNicknamePrefix(
            String nickname,
            Long gameId
    );
}

package com.hd.gamematch.gameuser.application.port.out;

import com.hd.gamematch.gameuser.domain.GameUserProfile;

import java.util.Optional;

public interface LoadGameUserPort {

    Optional<GameUserProfile> loadGameUserById(Long gameUserId);
}

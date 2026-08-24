package com.hd.gamematch.gameuser.application.port.in;

import com.hd.gamematch.gameuser.domain.GameUserProfile;

public interface FindGameUserUseCase {

    GameUserProfile findGameUser(FindGameUserQuery query);
}

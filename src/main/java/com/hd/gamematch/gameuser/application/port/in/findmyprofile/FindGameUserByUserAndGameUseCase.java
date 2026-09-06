package com.hd.gamematch.gameuser.application.port.in.findmyprofile;

import com.hd.gamematch.gameuser.domain.GameUserProfile;

public interface FindGameUserByUserAndGameUseCase {

    GameUserProfile findGameUser(FindGameUserByUserAndGameQuery query);
}

package com.hd.gamematch.gameuser.application.port.in.find;

import com.hd.gamematch.gameuser.domain.GameUserProfile;

import java.util.List;

public interface FindGameUserUseCase {

    GameUserProfile findGameUser(FindGameUserQuery query);

    List<GameUserProfile> findGameUsers(List<Long> gameUserIds);
}

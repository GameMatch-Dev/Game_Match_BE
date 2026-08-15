package com.hd.gamematch.gameuser.application.port.out;

import com.hd.gamematch.gameuser.domain.GameUser;

public interface SaveGameUserPort {

    void save(GameUser gameUser);
}

package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.FindGameUserByUserAndGameQuery;
import com.hd.gamematch.gameuser.application.port.in.FindGameUserByUserAndGameUseCase;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserByUserAndGamePort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindGameUserByUserAndGameService implements FindGameUserByUserAndGameUseCase {

    private final LoadGameUserByUserAndGamePort loadGameUserByUserAndGamePort;

    @Override
    public GameUserProfile findGameUser(FindGameUserByUserAndGameQuery query) {
        return loadGameUserByUserAndGamePort
                .loadGameUserByUserIdAndGameId(query.userId(), query.gameId())
                .orElseThrow(GameUserNotFoundException::new);
    }
}
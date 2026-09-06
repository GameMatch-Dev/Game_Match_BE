package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindGameUserService implements FindGameUserUseCase {

    private final LoadGameUserPort loadGameUserPort;

    @Override
    public GameUserProfile findGameUser(FindGameUserQuery query) {
        return loadGameUserPort.loadGameUserById(query.gameUserId())
                .orElseThrow(GameUserNotFoundException::new);
    }
}

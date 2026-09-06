package com.hd.gamematch.game.application.service;

import com.hd.gamematch.game.application.port.in.find.FindGamesQuery;
import com.hd.gamematch.game.application.port.in.find.FindGamesUseCase;
import com.hd.gamematch.game.application.port.out.LoadGamesPort;
import com.hd.gamematch.game.domain.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindGamesService implements FindGamesUseCase {

    private final LoadGamesPort loadGamesPort;

    @Override
    public List<Game> findGames(FindGamesQuery query) {
        if (query.hasName() && query.hasSort()) {
            return loadGamesPort.loadGamesByNameAndSort(query.name(), query.sort());
        }

        if (query.hasName()) {
            return loadGamesPort.loadGamesByName(query.name());
        }

        if (query.hasSort()) {
            return loadGamesPort.loadGamesBySort(query.sort());
        }

        return loadGamesPort.loadAllGames();
    }
}

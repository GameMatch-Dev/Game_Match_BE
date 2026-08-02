package com.hd.gamematch.game.application.service;

import com.hd.gamematch.game.application.port.in.FindGamesQuery;
import com.hd.gamematch.game.application.port.out.LoadGamesPort;
import com.hd.gamematch.game.domain.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FindGamesServiceTest {

    @Mock
    private LoadGamesPort loadGamesPort;

    @InjectMocks
    private FindGamesService findGamesService;

    private List<Game> games;

    @BeforeEach
    void setUp() {
        games = List.of(
                Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol")
        );
    }

    @Test
    void loadByNameAndSortWhenBothConditionsExist() {
        given(loadGamesPort.loadGamesByNameAndSort("League", "MOBA"))
                .willReturn(games);

        List<Game> result = findGamesService.findGames(
                FindGamesQuery.of("League", "MOBA")
        );

        assertSame(games, result);
        then(loadGamesPort).should().loadGamesByNameAndSort("League", "MOBA");
    }

    @Test
    void loadByNameWhenOnlyNameExists() {
        given(loadGamesPort.loadGamesByName("League"))
                .willReturn(games);

        List<Game> result = findGamesService.findGames(
                FindGamesQuery.of("League", null)
        );

        assertSame(games, result);
        then(loadGamesPort).should().loadGamesByName("League");
    }

    @Test
    void loadBySortWhenOnlySortExists() {
        given(loadGamesPort.loadGamesBySort("MOBA"))
                .willReturn(games);

        List<Game> result = findGamesService.findGames(
                FindGamesQuery.of(null, "MOBA")
        );

        assertSame(games, result);
        then(loadGamesPort).should().loadGamesBySort("MOBA");
    }

    @Test
    void loadAllWhenNoConditionExists() {
        given(loadGamesPort.loadAllGames())
                .willReturn(games);

        List<Game> result = findGamesService.findGames(
                FindGamesQuery.of(null, " ")
        );

        assertSame(games, result);
        then(loadGamesPort).should().loadAllGames();
    }
}

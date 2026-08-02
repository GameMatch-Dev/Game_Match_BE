package com.hd.gamematch.game.adapter.in.web;

import com.hd.gamematch.game.application.port.in.FindGameQuery;
import com.hd.gamematch.game.application.port.in.FindGameUseCase;
import com.hd.gamematch.game.application.port.in.FindGamesQuery;
import com.hd.gamematch.game.application.port.in.FindGamesUseCase;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.global.response.CommonResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private FindGamesUseCase findGamesUseCase;

    @Mock
    private FindGameUseCase findGameUseCase;

    @InjectMocks
    private GameController gameController;

    @Test
    void findGamesReturnsCommonResponseWithGameResponses() {
        Game game = Game.of(
                1L,
                "League of Legends",
                "MOBA",
                "https://example.com/lol"
        );
        given(findGamesUseCase.findGames(any(FindGamesQuery.class)))
                .willReturn(List.of(game));

        CommonResponse<List<GameResponse>> response =
                gameController.findGames(" League ", " MOBA ");

        assertTrue(response.success());
        assertEquals("SUCCESS", response.code());
        assertEquals("요청에 성공했습니다.", response.message());
        assertEquals(1, response.data().size());
        assertEquals(1L, response.data().get(0).id());
        assertEquals("League of Legends", response.data().get(0).name());
        assertEquals("MOBA", response.data().get(0).sort());
        assertEquals("https://example.com/lol", response.data().get(0).url());

        ArgumentCaptor<FindGamesQuery> queryCaptor =
                ArgumentCaptor.forClass(FindGamesQuery.class);
        then(findGamesUseCase).should().findGames(queryCaptor.capture());
        assertEquals("League", queryCaptor.getValue().name());
        assertEquals("MOBA", queryCaptor.getValue().sort());
    }

    @Test
    void findGameReturnsCommonResponseWithGameResponse() {
        Game game = Game.of(
                1L,
                "League of Legends",
                "MOBA",
                "https://example.com/lol"
        );
        given(findGameUseCase.findGame(any(FindGameQuery.class)))
                .willReturn(game);

        CommonResponse<GameResponse> response = gameController.findGame(1L);

        assertTrue(response.success());
        assertEquals("SUCCESS", response.code());
        assertEquals("요청에 성공했습니다.", response.message());
        assertEquals(1L, response.data().id());
        assertEquals("League of Legends", response.data().name());
        assertEquals("MOBA", response.data().sort());
        assertEquals("https://example.com/lol", response.data().url());

        ArgumentCaptor<FindGameQuery> queryCaptor =
                ArgumentCaptor.forClass(FindGameQuery.class);
        then(findGameUseCase).should().findGame(queryCaptor.capture());
        assertEquals(1L, queryCaptor.getValue().gameId());
    }
}

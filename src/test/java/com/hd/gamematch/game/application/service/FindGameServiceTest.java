package com.hd.gamematch.game.application.service;

import com.hd.gamematch.game.application.port.in.FindGameQuery;
import com.hd.gamematch.game.application.port.out.LoadGamePort;
import com.hd.gamematch.game.domain.Game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FindGameServiceTest {

    @Mock
    private LoadGamePort loadGamePort;

    @InjectMocks
    private FindGameService findGameService;

    @Test
    void findGameById() {
        Game game = Game.of(
                1L,
                "League of Legends",
                "MOBA",
                "https://example.com/lol"
        );
        given(loadGamePort.loadGameById(1L))
                .willReturn(Optional.of(game));

        Game result = findGameService.findGame(FindGameQuery.of(1L));

        assertEquals(1L, result.id());
        assertEquals("League of Legends", result.name());
        assertEquals("MOBA", result.sort());
        assertEquals("https://example.com/lol", result.url());

        then(loadGamePort).should().loadGameById(1L);
    }

    @Test
    void throwsIllegalArgumentExceptionWhenGameDoesNotExist() {
        given(loadGamePort.loadGameById(999L))
                .willReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> findGameService.findGame(FindGameQuery.of(999L))
        );

        assertEquals("게임을 찾을 수 없습니다.", exception.getMessage());
        then(loadGamePort).should().loadGameById(999L);
    }
}

package com.hd.gamematch.game.adapter.in.web;

import com.hd.gamematch.game.application.exception.GameNotFoundException;
import com.hd.gamematch.game.application.port.in.FindGameQuery;
import com.hd.gamematch.game.application.port.in.FindGameUseCase;
import com.hd.gamematch.game.application.port.in.FindGamesQuery;
import com.hd.gamematch.game.application.port.in.FindGamesUseCase;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 웹 계층만 띄워서 URL 요청이 HTTP 상태와 JSON으로 어떻게 변환되는지 확인한다.
// 서비스·DB 전체를 실행하는 통합 테스트보다 빠르고, 컨트롤러와 예외 처리기의 연결을 집중 검증한다.
@WebMvcTest(GameController.class)
@Import(GlobalExceptionHandler.class)
class GameControllerWebMvcTest {

    // 실제 웹 요청처럼 GET을 보내고, 응답 상태·헤더·JSON 필드를 검사하는 도구다.
    @Autowired
    private MockMvc mockMvc;

    // @WebMvcTest는 웹 빈만 만들므로, 컨트롤러가 필요로 하는 유스케이스는 MockitoBean 가짜 객체로 제공한다.
    @MockitoBean
    private FindGameUseCase findGameUseCase;

    @MockitoBean
    private FindGamesUseCase findGamesUseCase;

    @Test
    void findGameReturnsNotFoundResponseWhenGameDoesNotExist() throws Exception {
        // given-willThrow: 컨트롤러가 유스케이스를 호출하면 "게임 없음" 예외가 난다고 설정한다.
        // 이 설정으로 실제 DB 없이도 예외 처리 흐름을 재현할 수 있다.
        given(findGameUseCase.findGame(FindGameQuery.of(999L)))
                .willThrow(new GameNotFoundException());

        // status()는 HTTP 프로토콜의 404를, jsonPath()는 응답 본문의 각 JSON 값을 검증한다.
        // 둘을 모두 확인해야 "404를 보내면서도 약속된 응답 형식"을 지키는지 알 수 있다.
        mockMvc.perform(get("/games/{gameId}", 999L)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_001"))
                .andExpect(jsonPath("$.message").value("게임을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void findGameReturnsBadRequestResponseWhenGameIdIsMinusOne() throws Exception {
        // FindGameQuery.of(-1L)에서 검증 예외가 먼저 발생하므로 유스케이스 Mock 설정은 필요 없다.
        mockMvc.perform(get("/games/{gameId}", -1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("gameId는 1 이상이어야 합니다."))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void findGameReturnsBadRequestResponseWhenGameIdIsZero() throws Exception {
        // 0은 허용 범위 바로 밖의 경계값이므로, 음수값과 별도로 같은 400 계약을 확인한다.
        mockMvc.perform(get("/games/{gameId}", 0L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("gameId는 1 이상이어야 합니다."))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void findGameReturnsSuccessResponseWhenGameExists() throws Exception {
        // 새 404 처리가 기존의 정상 조회(200 OK) 결과를 바꾸지 않았는지 확인하는 회귀 테스트다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        given(findGameUseCase.findGame(FindGameQuery.of(1L))).willReturn(game);

        mockMvc.perform(get("/games/{gameId}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("League of Legends"));
    }

    @Test
    void findGamesReturnsSuccessResponseWithoutFilters() throws Exception {
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        given(findGamesUseCase.findGames(FindGamesQuery.of(null, null))).willReturn(java.util.List.of(game));

        mockMvc.perform(get("/games").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("League of Legends"))
                .andExpect(jsonPath("$.data[0].sort").value("MOBA"))
                .andExpect(jsonPath("$.data[0].url").value("https://example.com/lol"));

        then(findGamesUseCase).should().findGames(FindGamesQuery.of(null, null));
    }

    @Test
    void findGamesReturnsSuccessResponseWithNameFilter() throws Exception {
        Game game = Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol");
        given(findGamesUseCase.findGames(FindGamesQuery.of("League of Legends", null))).willReturn(java.util.List.of(game));

        mockMvc.perform(get("/games").param("name", "League of Legends").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].name").value("League of Legends"))
                .andExpect(jsonPath("$.data[0].sort").value("MOBA"))
                .andExpect(jsonPath("$.data[0].url").value("https://example.com/lol"));

        then(findGamesUseCase).should().findGames(FindGamesQuery.of("League of Legends", null));
    }

    @Test
    void findGamesReturnsSuccessResponseWithSortFilter() throws Exception {
        Game game = Game.of(3L, "Hades", "Roguelike", "https://example.com/hades");
        given(findGamesUseCase.findGames(FindGamesQuery.of(null, "Roguelike"))).willReturn(java.util.List.of(game));

        mockMvc.perform(get("/games").param("sort", "Roguelike").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].name").value("Hades"))
                .andExpect(jsonPath("$.data[0].sort").value("Roguelike"))
                .andExpect(jsonPath("$.data[0].url").value("https://example.com/hades"));

        then(findGamesUseCase).should().findGames(FindGamesQuery.of(null, "Roguelike"));
    }

    @Test
    void findGamesReturnsSuccessResponseWithNameAndSortFilters() throws Exception {
        Game game = Game.of(4L, "Overwatch 2", "FPS", "https://example.com/overwatch2");
        given(findGamesUseCase.findGames(FindGamesQuery.of("Overwatch 2", "FPS"))).willReturn(java.util.List.of(game));

        mockMvc.perform(get("/games")
                        .param("name", "Overwatch 2")
                        .param("sort", "FPS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(4))
                .andExpect(jsonPath("$.data[0].name").value("Overwatch 2"))
                .andExpect(jsonPath("$.data[0].sort").value("FPS"))
                .andExpect(jsonPath("$.data[0].url").value("https://example.com/overwatch2"));

        then(findGamesUseCase).should().findGames(FindGamesQuery.of("Overwatch 2", "FPS"));
    }
}

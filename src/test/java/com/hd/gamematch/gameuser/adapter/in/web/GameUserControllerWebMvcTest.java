package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.game.adapter.in.web.exception.GameExceptionHandler;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.adapter.in.web.exception.GameUserExceptionHandler;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.FindGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import com.hd.gamematch.game.application.port.in.FindGameUseCase;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = GameUserController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import({GlobalExceptionHandler.class, GameExceptionHandler.class, GameUserExceptionHandler.class})
class GameUserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindGameUseCase findGameUseCase;

    @MockitoBean
    private RegisterGameUserUseCase registerGameUserUseCase;

    @MockitoBean
    private FindGameUserUseCase findGameUserUseCase;

    @Test
    void 존재하는_게임_프로필을_조회하면_공개_응답을_반환한다() throws Exception {
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(1L))).willReturn(profile);

        mockMvc.perform(get("/game-users/{gameUserId}", 1L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("playerA"))
                .andExpect(jsonPath("$.data.game.id").value(2))
                .andExpect(jsonPath("$.data.user.id").value(3));
    }

    @Test
    void 존재하지_않는_게임_프로필을_조회하면_전용_404_응답을_반환한다() throws Exception {
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(999L)))
                .willThrow(new GameUserNotFoundException());

        mockMvc.perform(get("/game-users/{gameUserId}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_USER_001"))
                .andExpect(jsonPath("$.message").value("게임 프로필을 찾을 수 없습니다."));
    }

    @Test
    void 숫자가_아닌_게임_프로필_식별자를_조회하면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        then(findGameUserUseCase).should(never()).findGameUser(any(FindGameUserQuery.class));
    }

    @Test
    void 영인_게임_프로필_식별자를_조회하면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/{gameUserId}", 0L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("gameUserId는 1 이상이어야 합니다."));
    }
}

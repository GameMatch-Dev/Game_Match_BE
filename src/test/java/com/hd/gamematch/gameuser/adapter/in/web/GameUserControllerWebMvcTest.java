package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.game.adapter.in.web.exception.GameExceptionHandler;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.adapter.in.web.exception.GameUserExceptionHandler;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.in.findmyprofile.FindGameUserByUserAndGameQuery;
import com.hd.gamematch.gameuser.application.port.in.findmyprofile.FindGameUserByUserAndGameUseCase;
import com.hd.gamematch.gameuser.application.port.in.register.RegisterGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersQuery;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersResult;
import com.hd.gamematch.gameuser.application.port.in.search.SearchGameUsersUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import com.hd.gamematch.game.application.port.in.find.FindGameUseCase;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import com.hd.gamematch.support.security.SecuredWebMvcTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.util.List;
import java.util.stream.Stream;

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
@Import({GlobalExceptionHandler.class,
        GameExceptionHandler.class,
        GameUserExceptionHandler.class})
class GameUserControllerWebMvcTest extends SecuredWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindGameUseCase findGameUseCase;

    @MockitoBean
    private RegisterGameUserUseCase registerGameUserUseCase;

    @MockitoBean
    private FindGameUserUseCase findGameUserUseCase;

    @MockitoBean
    private FindGameUserByUserAndGameUseCase findGameUserByUserAndGameUseCase;

    @MockitoBean
    private SearchGameUsersUseCase searchGameUsersUseCase;

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


    @Test
    void 토큰_없이_현재_사용자의_게임_프로필을_조회하면_401_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/me/games/{gameId}", 2L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }


    @Test
    void 현재_사용자의_게임_프로필을_조회하면_최소_응답을_반환한다() throws Exception {
        // given
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );

        given(findGameUserByUserAndGameUseCase.findGameUser(
                FindGameUserByUserAndGameQuery.of(3L, 2L)
        )).willReturn(profile);


        // when & then
        mockMvc.perform(get("/game-users/me/games/{gameId}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("playerA"))
                .andExpect(jsonPath("$.data.game.id").value(2))
                .andExpect(jsonPath("$.data.game.name").value("League of Legends"))
                .andExpect(jsonPath("$.data.game.sort").value("MOBA"))
                .andExpect(jsonPath("$.data.game.url").value("https://example.com/lol"))
                .andExpect(jsonPath("$.data.user.id").value(3))
                .andExpect(jsonPath("$.data.user.email").doesNotExist())
                .andExpect(jsonPath("$.data.user.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.data.user.group").doesNotExist());
    }

    @Test
    void 게임_ID가_0이면_현재_사용자의_게임_프로필_조회에_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/me/games/{gameId}", 0L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void 게임_ID가_음수이면_현재_사용자의_게임_프로필_조회에_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/me/games/{gameId}", -1L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void 현재_사용자에게_해당_게임_프로필이_없으면_404_응답을_반환한다() throws Exception {
        given(findGameUserByUserAndGameUseCase.findGameUser(
                FindGameUserByUserAndGameQuery.of(3L, 2L)
        )).willThrow(new GameUserNotFoundException());

        mockMvc.perform(get("/game-users/me/games/{gameId}", 2L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_USER_001"));
    }


    @Test
    void 인증된_사용자가_닉네임으로_게임_프로필을_검색하면_200_응답을_반환한다() throws Exception {


        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );

        SearchGameUsersQuery query = SearchGameUsersQuery.of(
                "player",
                null,
                1,
                10
        );

        // given
        SearchGameUsersResult result = new SearchGameUsersResult(
                1L,
                List.of(profile)
        );


        given(searchGameUsersUseCase.searchGameUsers(query))
                .willReturn(result);

        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "player")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.gameUsers[0].id").value(1))
                .andExpect(jsonPath("$.data.gameUsers[0].nickname").value("playerA"))
                .andExpect(jsonPath("$.data.gameUsers[0].game.id").value(2))
                .andExpect(jsonPath("$.data.gameUsers[0].user.id").value(3));
    }

    @Test
    void 토큰_없이_게임_프로필을_검색하면_401_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "player")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void 닉네임이_공백이면_게임_프로필_검색에_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", " ")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void 닉네임_파라미터가_없으면_게임_프로필_검색에_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/search")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void 닉네임이_한_글자면_게임_프로필_검색에_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "p")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void 검색_결과가_없으면_200_응답과_빈_목록을_반환한다() throws Exception {
        SearchGameUsersQuery query = SearchGameUsersQuery.of("unknown", null, 1, 10);
        SearchGameUsersResult result = new SearchGameUsersResult(0L, List.of());

        given(searchGameUsersUseCase.searchGameUsers(query))
                .willReturn(result);

        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "unknown")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.gameUsers.length()").value(0));
    }

    @Test
    void 페이지_정보를_전달해_검색하면_해당_페이지의_응답_순서를_보존한다() throws Exception {
        // given: 전체 2건 중 두 번째 페이지(크기 1)에 해당하는 결과다.
        GameUserProfile secondProfile = new GameUserProfile(
                2L,
                "playerB",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                4L
        );
        SearchGameUsersQuery query = SearchGameUsersQuery.of("player", null, 2, 1);
        given(searchGameUsersUseCase.searchGameUsers(query))
                .willReturn(new SearchGameUsersResult(2L, List.of(secondProfile)));

        // when & then
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "player")
                        .param("page", "2")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.gameUsers.length()").value(1))
                .andExpect(jsonPath("$.data.gameUsers[0].id").value(2))
                .andExpect(jsonPath("$.data.gameUsers[0].nickname").value("playerB"));
    }

    @ParameterizedTest(name = "{0}={1}이면 400 응답을 반환한다")
    @MethodSource("잘못된_검색_조건")
    void 잘못된_검색_조건이면_400_응답을_반환한다(String parameterName, String parameterValue)
            throws Exception {
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "player")
                        .param(parameterName, parameterValue)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    private static Stream<Arguments> 잘못된_검색_조건() {
        return Stream.of(
                Arguments.of("gameId", "0"),
                Arguments.of("page", "0"),
                Arguments.of("size", "0"),
                Arguments.of("size", "21")
        );
    }
}

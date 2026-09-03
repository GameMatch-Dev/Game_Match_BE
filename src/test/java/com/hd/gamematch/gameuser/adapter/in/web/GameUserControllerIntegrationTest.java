package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private GameJpaRepository gameJpaRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private GameUserJpaRepository gameUserJpaRepository;

    @Test
    void 게임_프로필_등록_요청이_성공하면_프로필을_생성한다() throws Exception {
        // Given: 인증할 사용자와 등록 대상 게임을 테스트 DB에 준비한다.
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );

        String accessToken = jwtTokenService
                .issueAccessToken(savedUser.getId())
                .value();

        String requestBody = """
                {
                  "gameId": %d,
                  "nickname": "playerA"
                }
                """.formatted(savedGame.getId());

        // When & Then: JWT 인증 사용자가 게임 프로필 등록 API를 요청한다.
        mockMvc.perform(post("/game-users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.nickname").value("playerA"))
                .andExpect(jsonPath("$.data.game.id").value(savedGame.getId()))
                .andExpect(jsonPath("$.data.game.name").value("League of Legends"))
                .andExpect(jsonPath("$.data.game.sort").value("MOBA"))
                .andExpect(jsonPath("$.data.game.url").value("https://example.com/lol"))
                .andExpect(jsonPath("$.data.user.id").value(savedUser.getId()));
    }


    @Test
    void 같은_사용자가_같은_게임에_재등록하면_거부한다() throws Exception {
        // Given: 한 사용자와 게임, 그리고 인증 토큰을 준비한다.
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );

        String accessToken = jwtTokenService
                .issueAccessToken(savedUser.getId())
                .value();

        String requestBody = """
            {
              "gameId": %d,
              "nickname": "playerA"
            }
            """.formatted(savedGame.getId());

        // 같은 사용자의 첫 등록은 정상적으로 성공한다.
        mockMvc.perform(post("/game-users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // When & Then: 같은 사용자·같은 게임으로 다시 등록하면 거부한다.
        mockMvc.perform(post("/game-users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_USER_003"))
                .andExpect(jsonPath("$.message")
                        .value("이미 해당 게임에 게임 프로필이 등록되어 있습니다."))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        // 새 프로필이 추가 생성되지 않았는지도 확인한다.
        assertThat(gameUserJpaRepository.count()).isEqualTo(1);
    }

    // 서로 다른 사용자라도 같은 게임에서 이미 사용 중인 닉네임으로 등록할 수 없다.
    @Test
    void 같은_게임에서_닉네임이_중복되면_거부한다() throws Exception {
        // Given: 서로 다른 인증 사용자 두 명과 같은 게임을 준비한다.
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());

        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );

        String firstUserAccessToken = jwtTokenService
                .issueAccessToken(firstUser.getId())
                .value();

        String secondUserAccessToken = jwtTokenService
                .issueAccessToken(secondUser.getId())
                .value();

        String requestBody = """
            {
              "gameId": %d,
              "nickname": "playerA"
            }
            """.formatted(savedGame.getId());

        // 첫 사용자는 해당 닉네임으로 정상 등록한다.
        mockMvc.perform(post("/game-users")
                        .header("Authorization", "Bearer " + firstUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // When & Then: 다른 사용자라도 같은 게임에서 같은 닉네임은 사용할 수 없다.
        mockMvc.perform(post("/game-users")
                        .header("Authorization", "Bearer " + secondUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_USER_002"))
                .andExpect(jsonPath("$.message")
                        .value("이미 해당 게임에서 사용 중인 닉네임입니다."))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        // 닉네임 중복 요청 때문에 두 번째 프로필은 저장되지 않아야 한다.
        assertThat(gameUserJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 게임_프로필을_공개적으로_단건_조회한다() throws Exception {
        // Given: 조회할 사용자, 게임, 게임 프로필을 테스트 DB에 준비한다.
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());

        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );

        GameUserJpaEntity savedGameUser = gameUserJpaRepository.save(
                GameUserJpaEntity.of(
                        savedUser.getId(),
                        savedGame.getId(),
                        "playerA"
                )
        );

        // When & Then: 인증 없이도 약속한 최소 공개 프로필을 조회할 수 있다.
        mockMvc.perform(get("/game-users/{gameUserId}", savedGameUser.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.id").value(savedGameUser.getId()))
                .andExpect(jsonPath("$.data.nickname").value("playerA"))
                .andExpect(jsonPath("$.data.game.id").value(savedGame.getId()))
                .andExpect(jsonPath("$.data.game.name").value("League of Legends"))
                .andExpect(jsonPath("$.data.game.sort").value("MOBA"))
                .andExpect(jsonPath("$.data.game.url").value("https://example.com/lol"))
                .andExpect(jsonPath("$.data.user.id").value(savedUser.getId()));
    }

    @Test
    void 음수_게임_프로필_ID로_조회하면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/{gameUserId}", -1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증된_사용자가_자신의_게임_프로필을_조회한다() throws Exception {
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );
        GameUserJpaEntity savedGameUser = gameUserJpaRepository.save(
                GameUserJpaEntity.of(
                        savedUser.getId(),
                        savedGame.getId(),
                        "playerA"
                )
        );

        String accessToken = jwtTokenService
                .issueAccessToken(savedUser.getId())
                .value();

        mockMvc.perform(get("/game-users/me/games/{gameId}", savedGame.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(savedGameUser.getId()))
                .andExpect(jsonPath("$.data.nickname").value("playerA"))
                .andExpect(jsonPath("$.data.game.id").value(savedGame.getId()))
                .andExpect(jsonPath("$.data.user.id").value(savedUser.getId()));
    }

    @Test
    void 현재_사용자에게_해당_게임_프로필이_없으면_404_응답을_반환한다() throws Exception {
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );

        String accessToken = jwtTokenService
                .issueAccessToken(savedUser.getId())
                .value();

        mockMvc.perform(get("/game-users/me/games/{gameId}", savedGame.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_USER_001"));
    }

    @Test
    void 같은_게임에_다른_사용자의_프로필만_있으면_현재_사용자는_404_응답을_반환한다() throws Exception {
        UserJpaEntity currentUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity otherUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );
        gameUserJpaRepository.save(
                GameUserJpaEntity.of(
                        otherUser.getId(),
                        savedGame.getId(),
                        "otherPlayer"
                )
        );

        String accessToken = jwtTokenService
                .issueAccessToken(currentUser.getId())
                .value();

        mockMvc.perform(get("/game-users/me/games/{gameId}", savedGame.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_USER_001"));
    }

    @Test
    void 토큰_없이_현재_사용자의_게임_프로필을_조회하면_401_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/me/games/{gameId}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void 토큰_없이_게임_프로필을_검색하면_401_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "player")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void 인증된_사용자가_닉네임과_게임_조건으로_프로필을_검색한다() throws Exception {
        // given
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());
        gameUserJpaRepository.saveAll(java.util.List.of(
                GameUserJpaEntity.of(firstUser.getId(), savedGame.getId(), "playerA"),
                GameUserJpaEntity.of(secondUser.getId(), savedGame.getId(), "PlayerB")
        ));
        String accessToken = jwtTokenService.issueAccessToken(firstUser.getId()).value();

        // when & then
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "PLAYER")
                        .param("gameId", String.valueOf(savedGame.getId()))
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.gameUsers[0].nickname").value("playerA"))
                .andExpect(jsonPath("$.data.gameUsers[0].game.id").value(savedGame.getId()))
                .andExpect(jsonPath("$.data.gameUsers[0].user.id").value(firstUser.getId()))
                .andExpect(jsonPath("$.data.gameUsers[1].nickname").value("PlayerB"))
                .andExpect(jsonPath("$.data.gameUsers[1].user.id").value(secondUser.getId()));
    }

    @Test
    void 인증된_사용자가_게임_조건_없이_모든_게임에서_프로필을_검색한다() throws Exception {
        // given
        GameJpaEntity leagueOfLegends = gameJpaRepository.save(
                GameJpaEntity.of(
                        "League of Legends",
                        "MOBA",
                        "https://example.com/lol"
                )
        );
        GameJpaEntity valorant = gameJpaRepository.save(
                GameJpaEntity.of(
                        "Valorant",
                        "FPS",
                        "https://example.com/valorant"
                )
        );
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());
        gameUserJpaRepository.saveAll(java.util.List.of(
                GameUserJpaEntity.of(firstUser.getId(), leagueOfLegends.getId(), "playerA"),
                GameUserJpaEntity.of(secondUser.getId(), valorant.getId(), "PlayerB")
        ));
        String accessToken = jwtTokenService.issueAccessToken(firstUser.getId()).value();

        // when & then
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "PLAYER")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.gameUsers[0].nickname").value("playerA"))
                .andExpect(jsonPath("$.data.gameUsers[0].game.id").value(leagueOfLegends.getId()))
                .andExpect(jsonPath("$.data.gameUsers[1].nickname").value("PlayerB"))
                .andExpect(jsonPath("$.data.gameUsers[1].game.id").value(valorant.getId()));
    }
}

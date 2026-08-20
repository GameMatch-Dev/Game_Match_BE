package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

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
    void registerGameUserCreatesGameProfile() throws Exception {
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
    void registerGameUserRejectsDuplicateRegistrationForSameUserAndGame() throws Exception {
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
}

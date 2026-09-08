package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private EntityManager entityManager;

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
    void 인증된_사용자가_게임_프로필_닉네임을_수정한다() throws Exception {
        // given: 실제 사용자·게임·게임 프로필과 그 사용자의 JWT를 준비한다.
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        // PATCH 요청 전에 프로필 INSERT를 DB에 즉시 반영해, 이후 요청이 확실히 조회할 수 있게 한다.
        // (save만으로도 트랜잭션 종료 시 반영되지만, 이 테스트에서는 시작 데이터를 먼저 확정한다.)
        GameUserJpaEntity savedGameUser = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(savedUser.getId(), savedGame.getId(), "playerA")
        );
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();

        // when & then: Mock 없이 Security → Controller → Service → Persistence를 통과한다.
        mockMvc.perform(patch("/game-users/{gameUserId}", savedGameUser.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "newPlayer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(savedGameUser.getId()))
                .andExpect(jsonPath("$.data.nickname").value("newPlayer"))
                .andExpect(jsonPath("$.data.game.id").value(savedGame.getId()))
                .andExpect(jsonPath("$.data.user.id").value(savedUser.getId()));

        // flush: PATCH 과정에서 변경된 managed 엔티티의 UPDATE SQL을 DB에 동기화한다.
        entityManager.flush();
        // clear: 1차 캐시를 비워 아래 findById가 메모리 객체가 아닌 DB를 다시 조회하게 한다.
        entityManager.clear();

        assertThat(gameUserJpaRepository.findById(savedGameUser.getId()).orElseThrow().getNickname())
                .isEqualTo("newPlayer");
    }


    @Test
    void 다른_사용자가_게임_프로필_닉네임을_수정하면_403_응답을_반환하고_기존_닉네임을_유지한다() throws Exception {
        // given: 프로필 소유자와 수정 요청자를 서로 다른 사용자로 준비한다.
        UserJpaEntity owner = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity otherUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameUserJpaEntity savedGameUser = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(owner.getId(), savedGame.getId(), "playerA")
        );
        String otherUserAccessToken = jwtTokenService.issueAccessToken(otherUser.getId()).value();

        // when & then: 로그인에는 성공했어도, 프로필 주인이 아니면 수정할 수 없다.
        mockMvc.perform(patch("/game-users/{gameUserId}", savedGameUser.getId())
                        .header("Authorization", "Bearer " + otherUserAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "hijackedName"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_403"))
                .andExpect(jsonPath("$.message").value("게임 프로필을 수정할 권한이 없습니다."));

        // 캐시를 비우고 DB를 다시 읽어, 권한 없는 요청이 실제 데이터를 바꾸지 않았음을 검증한다.
        entityManager.flush();
        entityManager.clear();

        assertThat(gameUserJpaRepository.findById(savedGameUser.getId()).orElseThrow().getNickname())
                .isEqualTo("playerA");
    }

    @Test
    void 토큰_없이_게임_프로필_닉네임을_수정하면_401_응답을_반환한다() throws Exception {
        // JWT가 없으면 Controller와 Service에 도달하기 전에 Security가 요청을 차단한다.
        mockMvc.perform(patch("/game-users/{gameUserId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "newPlayer"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void 존재하지_않는_게임_프로필_닉네임을_수정하면_404_응답을_반환한다() throws Exception {
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();

        mockMvc.perform(patch("/game-users/{gameUserId}", 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "newPlayer"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_USER_001"))
                .andExpect(jsonPath("$.message").value("게임 프로필을 찾을 수 없습니다."));

        assertThat(gameUserJpaRepository.count()).isZero();
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
    void 인증됐지만_닉네임_파라미터가_없으면_공통_400_응답을_반환한다() throws Exception {
        // given
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();

        // when & then
        mockMvc.perform(get("/game-users/search")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
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

    @Test
    void 인증된_사용자가_일치하는_프로필이_없는_닉네임을_검색하면_빈_결과를_반환한다() throws Exception {
        // given
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();

        // when & then
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "unknown")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.gameUsers").isEmpty());
    }

    @Test
    void 인증된_사용자가_정렬된_검색_결과의_두번째_페이지를_조회한다() throws Exception {
        // given
        GameJpaEntity firstGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameJpaEntity secondGame = gameJpaRepository.save(
                GameJpaEntity.of("Valorant", "FPS", "https://example.com/valorant")
        );
        GameJpaEntity thirdGame = gameJpaRepository.save(
                GameJpaEntity.of("Overwatch", "FPS", "https://example.com/overwatch")
        );
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity thirdUser = userJpaRepository.save(UserJpaEntity.create());
        gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(firstUser.getId(), firstGame.getId(), "player")
        );
        GameUserJpaEntity secondProfile = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(secondUser.getId(), secondGame.getId(), "Player")
        );
        gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(thirdUser.getId(), thirdGame.getId(), "playerZ")
        );
        String accessToken = jwtTokenService.issueAccessToken(firstUser.getId()).value();

        // when & then
        mockMvc.perform(get("/game-users/search")
                        .param("nickname", "PLAYER")
                        .param("page", "2")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.gameUsers.length()").value(1))
                .andExpect(jsonPath("$.data.gameUsers[0].id").value(secondProfile.getId()))
                .andExpect(jsonPath("$.data.gameUsers[0].nickname").value("Player"));
    }
}

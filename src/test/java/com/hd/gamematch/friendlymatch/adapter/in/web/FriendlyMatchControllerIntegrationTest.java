package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaEntity;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FriendlyMatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private GameJpaRepository gameJpaRepository;

    @Autowired
    private GameUserJpaRepository gameUserJpaRepository;

    @Autowired
    private FriendlyMatchJpaRepository friendlyMatchJpaRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private EntityManager entityManager;

    // 인증된 사용자가 자신의 게임 프로필을 호스트로 친선 경기 모집을 생성한다.
    @Test
    void 인증된_사용자가_친선_경기_모집을_생성한다() throws Exception {
        // given: 인증 사용자와 그 사용자의 게임 프로필을 테스트 DB에 준비한다.
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameUserJpaEntity savedHost = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(savedUser.getId(), savedGame.getId(), "hostA")
        );
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();

        // when & then: JWT 인증부터 Controller, Service, Persistence를 거쳐 생성 결과를 반환한다.
        mockMvc.perform(post("/friendly-matches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %d,
                                  "gameId": %d,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "recruit": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """.formatted(savedHost.getId(), savedGame.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.host.id").value(savedHost.getId()))
                .andExpect(jsonPath("$.data.host.nickname").value("hostA"))
                .andExpect(jsonPath("$.data.game.id").value(savedGame.getId()))
                .andExpect(jsonPath("$.data.state").value(0));

        // flush와 clear 후 DB를 다시 읽어, HTTP 응답뿐 아니라 실제 저장값도 검증한다.
        entityManager.flush();
        entityManager.clear();

        FriendlyMatchJpaEntity savedFriendlyMatch = friendlyMatchJpaRepository.findAll().get(0);
        assertThat(savedFriendlyMatch.getHostGameUserId()).isEqualTo(savedHost.getId());
        assertThat(savedFriendlyMatch.getGameId()).isEqualTo(savedGame.getId());
        assertThat(savedFriendlyMatch.getRecruit()).isEqualTo(5);
        assertThat(savedFriendlyMatch.getComment()).isEqualTo("저녁에 같이 하실 분");
    }
}

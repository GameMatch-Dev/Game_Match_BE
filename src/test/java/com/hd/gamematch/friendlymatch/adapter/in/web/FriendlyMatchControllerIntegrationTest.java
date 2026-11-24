package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaEntity;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaRepository;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 실제 POST에서 명단 정책 오류를 공통 오류 응답으로 반환하고 경기와 명단 행을 남기지 않는다.
    @ParameterizedTest
    @CsvSource({"duplicate, 0, 400, COMMON_400", "missing, 0, 404, GAME_USER_001",
            "wronggame, 0, 400, COMMON_400", "overflow, 0, 400, COMMON_400",
            "overflow, 1, 400, COMMON_400"})
    void 잘못된_명단의_생성_요청은_오류를_반환하고_DB를_유지한다(
            String rosterCase, int sort, int expectedStatus, String expectedCode) throws Exception {
        UserJpaEntity owner = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity game = gameJpaRepository.save(
                GameJpaEntity.of("Roster error POST", "MOBA", "https://example.com/roster-error"));
        GameUserJpaEntity host = gameUserJpaRepository.save(
                GameUserJpaEntity.of(owner.getId(), game.getId(), "error-host"));
        List<Long> validMembers = new ArrayList<>();
        // 정원 초과 요청도 모두 같은 게임에 속한 서로 다른 실제 프로필로 구성한다.
        for (int index = 0; index < 6; index++) {
            UserJpaEntity member = userJpaRepository.save(UserJpaEntity.create());
            validMembers.add(gameUserJpaRepository.save(
                    GameUserJpaEntity.of(member.getId(), game.getId(), "error-member-" + index)).getId());
        }
        List<Long> requestedMembers;
        if (rosterCase.equals("wronggame")) {
            GameJpaEntity otherGame = gameJpaRepository.save(
                    GameJpaEntity.of("Other game", "FPS", "https://example.com/other"));
            UserJpaEntity otherOwner = userJpaRepository.save(UserJpaEntity.create());
            GameUserJpaEntity otherProfile = gameUserJpaRepository.save(
                    GameUserJpaEntity.of(otherOwner.getId(), otherGame.getId(), "other-game-member"));
            requestedMembers = List.of(validMembers.get(0), otherProfile.getId());
        } else {
            requestedMembers = switch (rosterCase) {
                case "duplicate" -> List.of(validMembers.get(0), validMembers.get(0));
                case "missing" -> List.of(validMembers.get(0), Long.MAX_VALUE);
                default -> validMembers;
            };
        }
        entityManager.flush();
        entityManager.clear();
        long originalMatchCount = friendlyMatchJpaRepository.count();
        Long originalRosterCount = jdbcTemplate.queryForObject(
                "select count(*) from friendly_match_host_member", Long.class);
        String token = jwtTokenService.issueAccessToken(owner.getId()).value();

        // 실제 JWT와 요청의 경기 유형을 사용해 생성 경로 전체의 오류 응답을 확인한다.
        mockMvc.perform(post("/friendly-matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %d,
                                  "gameId": %d,
                                  "date": "%s",
                                  "time": "20:00:00",
                                  "sort": %d,
                                  "capacity": 5,
                                  "comment": "명단 오류 검증",
                                  "hostTeamMemberIds": %s
                                }
                                """.formatted(host.getId(), game.getId(), LocalDate.now().plusDays(1),
                                sort, requestedMembers)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(expectedCode));

        // 오류 처리 뒤 캐시를 비우고 경기와 명단의 실제 행 수가 그대로인지 확인한다.
        entityManager.flush();
        entityManager.clear();
        assertThat(friendlyMatchJpaRepository.count()).isEqualTo(originalMatchCount);
        assertThat(jdbcTemplate.queryForObject("select count(*) from friendly_match_host_member", Long.class))
                .isEqualTo(originalRosterCount);
    }

    // 실제 POST에서 명단의 생략, null, 빈 목록, 순서와 호스트 포함 여부를 그대로 저장한다.
    @ParameterizedTest
    @ValueSource(strings = {"absent", "null", "empty", "members", "host"})
    void 생성_요청의_호스트_명단을_실제_DB에_저장한다(String rosterCase) throws Exception {
        UserJpaEntity hostUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity memberUserA = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity memberUserB = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity game = gameJpaRepository.save(
                GameJpaEntity.of("Roster POST", "MOBA", "https://example.com/roster"));
        GameUserJpaEntity host = gameUserJpaRepository.save(
                GameUserJpaEntity.of(hostUser.getId(), game.getId(), "roster-host"));
        GameUserJpaEntity memberA = gameUserJpaRepository.save(
                GameUserJpaEntity.of(memberUserA.getId(), game.getId(), "roster-member-a"));
        GameUserJpaEntity memberB = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(memberUserB.getId(), game.getId(), "roster-member-b"));
        List<Long> expectedMembers = switch (rosterCase) {
            case "members" -> List.of(memberB.getId(), memberA.getId());
            case "host" -> List.of(host.getId(), memberA.getId());
            default -> List.of();
        };
        String rosterField = switch (rosterCase) {
            case "absent" -> "";
            case "null" -> ", \"hostTeamMemberIds\": null";
            default -> ", \"hostTeamMemberIds\": " + expectedMembers;
        };
        String token = jwtTokenService.issueAccessToken(hostUser.getId()).value();

        // 인증부터 저장까지 실제 구성 요소를 통과시키고 공통 응답과 정원을 확인한다.
        MvcResult response = mockMvc.perform(post("/friendly-matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %d,
                                  "gameId": %d,
                                  "date": "%s",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "호스트 명단 POST 검증"%s
                                }
                                """.formatted(host.getId(), game.getId(), LocalDate.now().plusDays(1), rosterField)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.host.id").value(host.getId()))
                .andExpect(jsonPath("$.data.game.id").value(game.getId()))
                .andExpect(jsonPath("$.data.capacity").value(5))
                .andReturn();
        Long matchId = JsonPath.parse(response.getResponse().getContentAsString()).read("$.data.id", Long.class);

        // 영속성 문맥을 비운 뒤 응답 ID로 실제 행과 명단 순서를 직접 조회한다.
        entityManager.flush();
        entityManager.clear();
        assertThat(jdbcTemplate.queryForObject("select host_roster_confirmed from friendly_match where id = ?",
                Boolean.class, matchId)).isEqualTo(!rosterCase.equals("absent") && !rosterCase.equals("null"));
        assertThat(jdbcTemplate.queryForObject("select host_game_user_id from friendly_match where id = ?",
                Long.class, matchId)).isEqualTo(host.getId());
        assertThat(jdbcTemplate.queryForObject("select game_id from friendly_match where id = ?",
                Long.class, matchId)).isEqualTo(game.getId());
        assertThat(jdbcTemplate.queryForObject("select capacity from friendly_match where id = ?",
                Integer.class, matchId)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForList(
                "select game_user_id from friendly_match_host_member where match_id = ? order by member_order",
                Long.class, matchId)).containsExactlyElementsOf(expectedMembers);
    }

    // 팀전과 개인전 모두 요청 숫자가 DB enum과 응답 숫자로 올바르게 연결된다.
    @ParameterizedTest
    @CsvSource({"0, TEAM", "1, INDIVIDUAL"})
    void 인증된_사용자가_친선_경기_모집을_생성한다(int sort, FriendlyMatchSort expectedSort) throws Exception {
        // given: 인증 사용자와 그 사용자의 게임 프로필을 테스트 DB에 준비한다.
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameUserJpaEntity savedHost = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(savedUser.getId(), savedGame.getId(), "hostA")
        );
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();
        // 시간이 지나도 정상 요청이 과거 일시가 되지 않도록 미래 날짜를 사용한다.
        LocalDate matchDate = LocalDate.now().plusDays(1);

        // when & then: JWT 인증부터 Controller, Service, Persistence를 거쳐 생성 결과를 반환한다.
        mockMvc.perform(post("/friendly-matches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %d,
                                  "gameId": %d,
                                  "date": "%s",
                                  "time": "20:00:00",
                                  "sort": %d,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """.formatted(savedHost.getId(), savedGame.getId(), matchDate, sort)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.host.id").value(savedHost.getId()))
                .andExpect(jsonPath("$.data.host.nickname").value("hostA"))
                .andExpect(jsonPath("$.data.game.id").value(savedGame.getId()))
                .andExpect(jsonPath("$.data.capacity").value(5))
                .andExpect(jsonPath("$.data.sort").value(sort))
                .andExpect(jsonPath("$.data.state").value(0));

        // flush와 clear 후 DB를 다시 읽어, HTTP 응답뿐 아니라 실제 저장값도 검증한다.
        entityManager.flush();
        entityManager.clear();

        FriendlyMatchJpaEntity savedFriendlyMatch = friendlyMatchJpaRepository.findAll().get(0);
        assertThat(savedFriendlyMatch.getHostGameUserId()).isEqualTo(savedHost.getId());
        assertThat(savedFriendlyMatch.getGameId()).isEqualTo(savedGame.getId());
        assertThat(savedFriendlyMatch.getCapacity()).isEqualTo(5);
        assertThat(savedFriendlyMatch.getSort()).isEqualTo(expectedSort);
        assertThat(savedFriendlyMatch.getComment()).isEqualTo("저녁에 같이 하실 분");
    }

    // 실제 인증과 Service를 거쳐 과거 경기 요청이 거부되고 DB에도 남지 않는지 확인한다.
    @Test
    void 과거_경기_일시로_생성하면_400을_반환하고_저장하지_않는다() throws Exception {
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameUserJpaEntity savedHost = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(savedUser.getId(), savedGame.getId(), "hostA")
        );
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();

        // 어제 날짜를 사용해 실행 시각이나 초 단위 경계에 영향받지 않게 한다.
        mockMvc.perform(post("/friendly-matches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %d,
                                  "gameId": %d,
                                  "date": "%s",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "과거 경기 요청"
                                }
                                """.formatted(savedHost.getId(), savedGame.getId(), LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // 저장 전 정책 검증에서 중단되어 매치가 생성되지 않아야 한다.
        entityManager.flush();
        entityManager.clear();
        assertThat(friendlyMatchJpaRepository.count()).isZero();
    }

    // 토큰 없이 생성 요청하면 Security가 Controller 전에 요청을 차단한다.
    @Test
    void 토큰_없이_친선_경기_모집을_생성하면_401_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/friendly-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        // 인증 단계에서 차단되므로 친선 경기 데이터는 저장되지 않아야 한다.
        assertThat(friendlyMatchJpaRepository.count()).isZero();
    }

    // 존재하지 않는 호스트 게임 프로필로는 친선 경기 모집을 생성할 수 없다.
    @Test
    void 존재하지_않는_호스트_게임_프로필로_친선_경기_모집을_생성하면_404_응답을_반환한다() throws Exception {
        // given: 인증 사용자는 있지만, 요청 본문의 호스트 게임 프로필은 DB에 만들지 않는다.
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.create());
        String accessToken = jwtTokenService.issueAccessToken(savedUser.getId()).value();

        // when & then: Service의 GameUserNotFoundException이 공통 예외 처리기를 거쳐 404로 변환된다.
        mockMvc.perform(post("/friendly-matches")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 999999,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_USER_001"))
                .andExpect(jsonPath("$.message").value("게임 프로필을 찾을 수 없습니다."));

        // 호스트 검증에서 중단되므로 친선 경기 데이터는 저장되지 않아야 한다.
        assertThat(friendlyMatchJpaRepository.count()).isZero();
    }

    // 다른 사용자의 게임 프로필을 호스트로 지정하면 친선 경기 모집을 만들 수 없다.
    @Test
    void 타인의_게임_프로필로_친선_경기_모집을_생성하면_403_응답을_반환한다() throws Exception {
        // given: 호스트 프로필 소유자와 생성 요청 사용자를 서로 다르게 준비한다.
        UserJpaEntity hostOwner = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity requester = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity savedGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameUserJpaEntity anotherUsersHost = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(hostOwner.getId(), savedGame.getId(), "hostA")
        );
        String requesterAccessToken = jwtTokenService.issueAccessToken(requester.getId()).value();

        // when & then: 인증에는 성공해도 호스트 프로필 소유자가 아니면 403으로 거부한다.
        mockMvc.perform(post("/friendly-matches")
                        .header("Authorization", "Bearer " + requesterAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %d,
                                  "gameId": %d,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """.formatted(anotherUsersHost.getId(), savedGame.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_403"))
                .andExpect(jsonPath("$.message").value("친선 경기 모집을 만들 권한이 없습니다."));

        // 권한이 없는 요청은 친선 경기 테이블에 아무 행도 남기면 안 된다.
        assertThat(friendlyMatchJpaRepository.count()).isZero();
    }
}

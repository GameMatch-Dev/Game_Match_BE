package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaEntity;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaRepository;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FriendlyMatchHostRosterControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private GameJpaRepository gameRepository;
    @Autowired private GameUserJpaRepository profileRepository;
    @Autowired private FriendlyMatchJpaRepository matchRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Game game;
    private GameUserProfile host;
    private GameUserProfile firstMember;
    private GameUserProfile secondMember;
    private GameUserProfile thirdMember;
    private Long matchId;
    private String hostToken;
    private String otherUserToken;
    private Map<String, Object> originalMetadata;
    private long originalMatchCount;

    // 실제 사용자와 프로필 및 확정된 기존 명단을 저장하고 소유자별 JWT를 발급한다.
    @BeforeEach
    void 기존_호스트_명단과_실제_JWT를_준비한다() {
        GameJpaEntity savedGame = gameRepository.saveAndFlush(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol"));
        game = Game.of(savedGame.getId(), savedGame.getName(), savedGame.getSort(), savedGame.getUrl());
        // 사용자 ID와 프로필 ID의 우연한 일치로 소유자 검증 오류가 가려지지 않게 한다.
        userRepository.saveAndFlush(UserJpaEntity.create());
        host = saveProfile("host");
        firstMember = saveProfile("memberA");
        secondMember = saveProfile("memberB");
        thirdMember = saveProfile("memberC");
        assertThat(host.userId()).isNotEqualTo(host.id());
        FriendlyMatch match = new FriendlyMatch(null, host, game, LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0), FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING,
                5, "함께 경기해요");
        FriendlyMatchJpaEntity entity = FriendlyMatchJpaEntity.from(match);
        entity.confirmHostRoster(List.of(firstMember.id()));
        matchId = matchRepository.saveAndFlush(entity).getId();
        hostToken = jwtTokenService.issueAccessToken(host.userId()).value();
        otherUserToken = jwtTokenService.issueAccessToken(secondMember.userId()).value();
        flushAndClear();
        originalMetadata = storedMetadata();
        originalMatchCount = matchRepository.count();
    }

    // 실제 JWT 인증부터 명단 저장까지 연결하고 경기 응답의 식별자와 팀별 정원을 확인한다.
    @Test
    void 소유자의_명단_확정은_200과_경기_정보를_반환한다() throws Exception {
        // 최초 확정 전 상태에서 실제 PUT이 명단과 확정 표시를 함께 저장하는지 확인한다.
        flushAndClear();
        jdbcTemplate.update("delete from friendly_match_host_member where match_id = ?", matchId);
        jdbcTemplate.update("update friendly_match set host_roster_confirmed = ? where id = ?", false, matchId);
        flushAndClear();
        assertThat(jdbcTemplate.queryForList(
                "select game_user_id from friendly_match_host_member where match_id = ?",
                Long.class, matchId)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "select host_roster_confirmed from friendly_match where id = ?", Boolean.class, matchId)).isFalse();
        List<Long> members = List.of(thirdMember.id(), secondMember.id());

        assertSuccess(request(matchId, hostToken, body(members)));

        assertStoredRoster(members);
    }

    // 반복 PUT은 이전 팀원 행을 남기지 않고 새 명단으로 교체하며 경기 정보를 유지한다.
    @Test
    void 반복_PUT은_기존_명단을_교체하고_경기_정보를_유지한다() throws Exception {
        assertSuccess(request(matchId, hostToken, body(List.of(secondMember.id(), firstMember.id()))));
        assertStoredRoster(List.of(secondMember.id(), firstMember.id()));

        assertSuccess(request(matchId, hostToken, body(List.of(thirdMember.id()))));

        assertStoredRoster(List.of(thirdMember.id()));
    }

    // JWT가 없는 요청은 Security에서 거부하며 기존 명단과 확정 표시를 유지한다.
    @Test
    void JWT가_없으면_401이고_기존_명단을_유지한다() throws Exception {
        mockMvc.perform(put("/friendly-matches/{matchId}/host-roster", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of(secondMember.id()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        assertStoredRoster(List.of(firstMember.id()));
    }

    // 다른 실제 사용자의 JWT는 소유자 확인에서 거부하며 저장된 명단을 변경하지 않는다.
    @Test
    void 다른_사용자의_JWT는_403이고_기존_명단을_유지한다() throws Exception {
        request(matchId, otherUserToken, body(List.of(thirdMember.id())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        assertStoredRoster(List.of(firstMember.id()));
    }

    // 없는 경기 요청은 404이며 기존 경기와 명단 외에 새 행을 만들지 않는다.
    @Test
    void 없는_경기는_404이고_기존_명단을_유지한다() throws Exception {
        request(Long.MAX_VALUE, hostToken, body(List.of(secondMember.id())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRIENDLY_MATCH_001"));

        assertStoredRoster(List.of(firstMember.id()));
    }

    // 필수 목록 누락과 null 및 유효하지 않은 원소는 저장 전에 400으로 거부한다.
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"teamMemberIds\":null}", "{\"teamMemberIds\":[0]}",
            "{\"teamMemberIds\":[null]}", "{\"teamMemberIds\":[1.5]}"})
    void 유효하지_않은_본문은_400이고_기존_명단을_유지한다(String invalidBody) throws Exception {
        request(matchId, hostToken, invalidBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        assertStoredRoster(List.of(firstMember.id()));
    }

    // 각 프로필을 서로 다른 실제 사용자에게 연결한다.
    private GameUserProfile saveProfile(String nickname) {
        UserJpaEntity user = userRepository.saveAndFlush(UserJpaEntity.create());
        GameUserJpaEntity profile = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(user.getId(), game.id(), nickname));
        return new GameUserProfile(profile.getId(), nickname, game, user.getId());
    }

    // 실제 발급한 JWT를 Authorization 헤더에 넣어 PUT 요청을 보낸다.
    private ResultActions request(Long targetMatchId, String token, String requestBody) throws Exception {
        return mockMvc.perform(put("/friendly-matches/{matchId}/host-roster", targetMatchId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    // 비어 있지 않은 팀원 목록을 요청 JSON으로 변환한다.
    private String body(List<Long> members) {
        return "{\"teamMemberIds\":" + members + "}";
    }

    // 성공 응답이 저장한 경기와 호스트 프로필 및 게임을 가리키는지 확인한다.
    private void assertSuccess(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(matchId))
                .andExpect(jsonPath("$.data.host.id").value(host.id()))
                .andExpect(jsonPath("$.data.game.id").value(game.id()))
                .andExpect(jsonPath("$.data.capacity").value(5));
    }

    // 캐시를 비운 뒤 실제 DB의 명단 순서와 행 수 및 확정 표시와 경기 정보를 확인한다.
    private void assertStoredRoster(List<Long> expectedMembers) {
        flushAndClear();
        assertThat(jdbcTemplate.queryForList(
                "select game_user_id from friendly_match_host_member where match_id = ? order by member_order",
                Long.class, matchId)).containsExactlyElementsOf(expectedMembers);
        assertThat(jdbcTemplate.queryForList(
                "select member_order from friendly_match_host_member where match_id = ? order by member_order",
                Integer.class, matchId)).containsExactlyElementsOf(
                        java.util.stream.IntStream.range(0, expectedMembers.size()).boxed().toList());
        assertThat(jdbcTemplate.queryForObject("select count(*) from friendly_match_host_member", Long.class))
                .isEqualTo((long) expectedMembers.size());
        assertThat(jdbcTemplate.queryForObject(
                "select host_roster_confirmed from friendly_match where id = ?", Boolean.class, matchId)).isTrue();
        assertThat(storedMetadata()).isEqualTo(originalMetadata);
        assertThat(matchRepository.count()).isEqualTo(originalMatchCount);
    }

    // 명단 외 경기 필드를 SQL로 읽어 요청 전후 값을 비교한다.
    private Map<String, Object> storedMetadata() {
        return jdbcTemplate.queryForMap(
                "select id, host_game_user_id, game_id, match_date, match_time, sort, state, capacity, comment "
                        + "from friendly_match where id = ?", matchId);
    }

    // 지연된 SQL을 실행하고 영속성 문맥의 캐시를 제거한다.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

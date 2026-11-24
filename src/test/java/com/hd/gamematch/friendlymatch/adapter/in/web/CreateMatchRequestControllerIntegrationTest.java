package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaEntity;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaRepository;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreateMatchRequestControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private GameJpaRepository gameRepository;
    @Autowired private GameUserJpaRepository profileRepository;
    @Autowired private FriendlyMatchJpaRepository matchRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Game game;
    private GameUserProfile requester;
    private GameUserProfile firstMember;
    private GameUserProfile secondMember;
    private GameUserProfile thirdMember;
    private Long matchId;
    private String accessToken;

    // 실제 소유자가 있는 프로필과 모집 중인 경기를 준비하고 JWT를 발급한다.
    @BeforeEach
    void 신청할_경기와_사용자를_준비한다() {
        GameJpaEntity savedGame = gameRepository.saveAndFlush(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol"));
        game = Game.of(savedGame.getId(), savedGame.getName(), savedGame.getSort(), savedGame.getUrl());
        GameUserProfile host = saveProfile(game, "host");
        requester = saveProfile(game, "requester");
        firstMember = saveProfile(game, "memberA");
        secondMember = saveProfile(game, "memberB");
        thirdMember = saveProfile(game, "memberC");
        FriendlyMatch match = new FriendlyMatch(null, host, game, LocalDate.now().plusDays(10),
                LocalTime.of(20, 0), FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING,
                5, "함께 경기해요");
        matchId = matchRepository.saveAndFlush(FriendlyMatchJpaEntity.from(match)).getId();
        accessToken = jwtTokenService.issueAccessToken(requester.userId()).value();
        flushAndClear();
    }

    // 실제 JWT부터 저장까지 연결하고 다른 소유자의 팀원 순서도 보존한다.
    @Test
    void 새_팀_신청은_201과_저장된_ID를_반환한다() throws Exception {
        ResultActions response = request(matchId, requester.id(),
                "[%d,%d]".formatted(secondMember.id(), firstMember.id()), "함께 신청합니다")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        flushAndClear();
        Long requestId = storedRequestId();
        response.andExpect(jsonPath("$.data.requestId").value(requestId));
        assertStoredRequest(requestId, MatchRequestState.PENDING, "함께 신청합니다",
                List.of(secondMember.id(), firstMember.id()));
        assertThat(requestCount()).isEqualTo(1);
        // 신청만으로 모집 정원이나 모집 상태가 변경되지 않는다.
        assertThat(jdbcTemplate.queryForObject("select capacity from friendly_match where id = ?",
                Integer.class, matchId)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("select state from friendly_match where id = ?",
                String.class, matchId)).isEqualTo("RECRUITING");
    }

    // 팀원 목록의 생략과 null 및 빈 배열을 모두 개인 신청으로 처리한다.
    @ParameterizedTest
    @ValueSource(strings = {"omitted", "null", "[]"})
    void 개인_신청은_팀원_행_없이_201을_반환한다(String members) throws Exception {
        request(matchId, requester.id(), members, "혼자 신청합니다")
                .andExpect(status().isCreated());

        flushAndClear();
        assertThat(requestCount()).isEqualTo(1);
        assertStoredRequest(storedRequestId(), MatchRequestState.PENDING, "혼자 신청합니다", List.of());
    }

    // 재신청은 기존 ID로 응답하고 이전 명단과 소개를 현재 값으로 교체한다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"CANCELLED", "REJECTED"})
    void 취소되거나_거절된_신청은_200으로_재활성화한다(MatchRequestState previousState) throws Exception {
        Long requestId = seedRequest(previousState);

        request(matchId, requester.id(), "[%d,%d]".formatted(thirdMember.id(), firstMember.id()), "새 소개")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(requestId));

        flushAndClear();
        assertThat(requestCount()).isEqualTo(1);
        assertStoredRequest(requestId, MatchRequestState.PENDING, "새 소개",
                List.of(thirdMember.id(), firstMember.id()));
    }

    // 토큰이 없으면 실제 Security가 차단하며 신청 테이블에 아무 행도 남지 않는다.
    @Test
    void 토큰_없는_신청은_401이다() throws Exception {
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(requester.id(), "[]", "신청")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
        assertNoRequests();
    }

    // JWT 사용자와 신청 프로필 소유자가 다르면 저장 전에 거부한다.
    @Test
    void 타인의_프로필로_신청하면_403이다() throws Exception {
        request(matchId, firstMember.id(), "[]", "신청")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
        assertNoRequests();
    }

    // 존재하지 않는 신청자 프로필은 실제 조회에서 404로 변환된다.
    @Test
    void 없는_신청_프로필은_404이다() throws Exception {
        request(matchId, Long.MAX_VALUE, "[]", "신청")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_USER_001"));
        assertNoRequests();
    }

    // 존재하지 않는 경기에는 신청을 저장하지 않는다.
    @Test
    void 없는_경기는_404이다() throws Exception {
        request(Long.MAX_VALUE, requester.id(), "[]", "신청")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRIENDLY_MATCH_001"));
        assertNoRequests();
    }

    // 활성 신청의 중복 요청은 기존 상태와 명단 및 소개를 그대로 유지한다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"PENDING", "APPROVED"})
    void 활성_신청이_있으면_409이고_기존_내용을_유지한다(MatchRequestState state) throws Exception {
        Long requestId = seedRequest(state);

        request(matchId, requester.id(), "[%d]".formatted(thirdMember.id()), "바뀌면 안 되는 소개")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_002"));

        flushAndClear();
        assertThat(requestCount()).isEqualTo(1);
        assertStoredRequest(requestId, state, "이전 소개", List.of(firstMember.id(), secondMember.id()));
    }

    // 신청자와 팀원 어느 쪽이든 경기와 다른 게임이면 저장하지 않는다.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 다른_게임의_프로필은_400이다(boolean wrongRequester) throws Exception {
        GameJpaEntity other = gameRepository.saveAndFlush(
                GameJpaEntity.of("Other Game", "SPORTS", "https://example.com/other"));
        GameUserJpaEntity wrongProfile = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(requester.userId(), other.getId(), "otherGame"));
        flushAndClear();

        request(matchId, wrongRequester ? wrongProfile.getId() : requester.id(),
                wrongRequester ? "[]" : "[%d]".formatted(wrongProfile.getId()), "신청")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        assertNoRequests();
    }

    // 팀원끼리의 중복과 신청자 자신을 팀원에 넣는 중복을 함께 거부한다.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void 중복_참가_프로필은_400이다(boolean includesRequester) throws Exception {
        String members = includesRequester ? "[%d]".formatted(requester.id())
                : "[%d,%d]".formatted(firstMember.id(), firstMember.id());
        request(matchId, requester.id(), members, "신청")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        assertNoRequests();
    }

    // 앞의 팀원이 유효해도 뒤의 팀원이 없으면 신청과 팀원을 일부 저장하지 않는다.
    @Test
    void 없는_팀원이_포함되면_404이고_부분_저장이_없다() throws Exception {
        request(matchId, requester.id(), "[%d,%d]".formatted(firstMember.id(), Long.MAX_VALUE), "신청")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_USER_001"));
        assertNoRequests();
    }

    // 각 프로필을 서로 다른 실제 사용자에게 연결한다.
    private GameUserProfile saveProfile(Game profileGame, String nickname) {
        UserJpaEntity owner = userRepository.saveAndFlush(UserJpaEntity.create());
        GameUserJpaEntity profile = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(owner.getId(), profileGame.id(), nickname));
        return new GameUserProfile(profile.getId(), nickname, profileGame, owner.getId());
    }

    // 실제 JWT를 첨부하여 신청 HTTP 경로를 호출한다.
    private ResultActions request(Long targetMatchId, Long profileId, String members, String comment)
            throws Exception {
        return mockMvc.perform(post("/friendly-matches/{matchId}/requests", targetMatchId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body(profileId, members, comment)));
    }

    // 생략된 목록과 명시적인 JSON 목록을 구분하여 요청 본문을 만든다.
    private String body(Long profileId, String members, String comment) {
        String memberField = members.equals("omitted") ? "" : ",\"teamMemberIds\":" + members;
        return "{\"gameUserId\":%d%s,\"comment\":\"%s\"}".formatted(profileId, memberField, comment);
    }

    // 취소 및 승인 API 호출을 흉내 내지 않고 기존 상태를 DB fixture로만 준비한다.
    private Long seedRequest(MatchRequestState state) {
        jdbcTemplate.update("insert into match_request (match_id, game_user_id, state, comment) values (?, ?, ?, ?)",
                matchId, requester.id(), state.name(), "이전 소개");
        Long requestId = storedRequestId();
        jdbcTemplate.update("insert into match_request_member (request_id, game_user_id, member_order) values (?, ?, ?)",
                requestId, firstMember.id(), 0);
        jdbcTemplate.update("insert into match_request_member (request_id, game_user_id, member_order) values (?, ?, ?)",
                requestId, secondMember.id(), 1);
        flushAndClear();
        return requestId;
    }

    // Entity 조회에 의존하지 않고 DB 헤더와 명단 및 저장 순서를 직접 확인한다.
    private void assertStoredRequest(Long requestId, MatchRequestState state, String comment, List<Long> members) {
        MatchRequest header = jdbcTemplate.queryForObject(
                "select id, match_id, game_user_id, state, comment from match_request where id = ?",
                (row, index) -> new MatchRequest(row.getLong("id"), row.getLong("match_id"),
                        row.getLong("game_user_id"), List.of(), row.getString("comment"),
                        MatchRequestState.valueOf(row.getString("state"))), requestId);
        assertThat(header).isEqualTo(new MatchRequest(requestId, matchId, requester.id(), List.of(), comment, state));
        assertThat(jdbcTemplate.queryForList(
                "select game_user_id from match_request_member where request_id = ? order by member_order",
                Long.class, requestId)).containsExactlyElementsOf(members);
        assertThat(jdbcTemplate.queryForList(
                "select member_order from match_request_member where request_id = ? order by member_order",
                Integer.class, requestId)).containsExactlyElementsOf(
                        java.util.stream.IntStream.range(0, members.size()).boxed().toList());
        assertThat(jdbcTemplate.queryForObject("select count(*) from match_request_member", Long.class))
                .isEqualTo((long) members.size());
    }

    // 현재 경기와 신청자에 저장된 신청 ID를 DB에서 조회한다.
    private Long storedRequestId() {
        return jdbcTemplate.queryForObject("select id from match_request where match_id = ? and game_user_id = ?",
                Long.class, matchId, requester.id());
    }

    // 신규 저장이나 중복 저장 여부를 DB 행 수로 확인한다.
    private Long requestCount() {
        return jdbcTemplate.queryForObject("select count(*) from match_request", Long.class);
    }

    // 실패한 요청이 신청 헤더나 팀원 행을 남기지 않았는지 확인한다.
    private void assertNoRequests() {
        flushAndClear();
        assertThat(requestCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from match_request_member", Long.class)).isZero();
    }

    // SQL 반영을 마치고 영속성 문맥의 캐시를 비운다.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

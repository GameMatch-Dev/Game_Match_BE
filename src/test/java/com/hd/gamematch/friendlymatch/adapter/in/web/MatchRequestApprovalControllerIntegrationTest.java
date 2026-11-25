package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaEntity;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchJpaRepository;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.MatchRequestJpaEntity;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.MatchRequestJpaRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 JWT와 전체 승인 경로를 검증한다. 테스트 트랜잭션의 독립 커밋과 경합 검증은 별도다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchRequestApprovalControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private GameJpaRepository gameRepository;
    @Autowired private GameUserJpaRepository profileRepository;
    @Autowired private FriendlyMatchJpaRepository matchRepository;
    @Autowired private MatchRequestJpaRepository requestRepository;
    @Autowired private JwtTokenService tokenService;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Game game;
    private GameUserProfile host;
    private GameUserProfile hostMember;
    private GameUserProfile representative;
    private GameUserProfile teammate;
    private Long matchId;
    private Long requestId;
    private String hostToken;

    // 실제 사용자와 프로필을 준비하며 주최자는 자동 참가자로 등록하지 않는다.
    @BeforeEach
    void 승인_요청에_필요한_실제_데이터를_준비한다() {
        GameJpaEntity savedGame = gameRepository.saveAndFlush(GameJpaEntity.of("League of Legends", "MOBA", null));
        game = Game.of(savedGame.getId(), savedGame.getName(), savedGame.getSort(), savedGame.getUrl());
        userRepository.saveAndFlush(UserJpaEntity.create());
        host = profile("host");
        hostMember = profile("hostMember");
        representative = profile("representative");
        teammate = profile("teammate");
        hostToken = tokenService.issueAccessToken(host.userId()).value();
        matchId = match(FriendlyMatchSort.TEAM, 5, true, List.of(hostMember.id()));
        requestId = request(matchId, representative.id(), List.of(teammate.id()), MatchRequestState.PENDING);
        flushAndClear();
    }

    // HTTP 승인으로 대표와 팀원을 저장하고 TEAM 모집을 종료한다.
    @Test
    void 팀전_승인은_실제_참가자와_승인_상태를_저장한다() throws Exception {
        approve(requestId, hostToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.matchId").value(matchId))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.participantGameUserIds[0]").value(representative.id()))
                .andExpect(jsonPath("$.data.participantGameUserIds[1]").value(teammate.id()));
        flushAndClear();
        assertThat(participants(requestId)).containsExactly(representative.id(), teammate.id());
        assertThat(requestState(requestId)).isEqualTo(MatchRequestState.APPROVED);
        assertThat(matchState(matchId)).isEqualTo(FriendlyMatchState.RECRUITMENT_CLOSED);
        assertThat(hostIds(matchId)).containsExactly(hostMember.id());
    }

    // 개인전은 주최 실제 참가자를 포함한 전체 인원이 정원에 도달할 때 모집을 종료한다.
    @ParameterizedTest
    @ValueSource(ints = {3, 4})
    void 개인전은_전체_정원에_따라_모집_상태를_반영한다(int capacity) throws Exception {
        Long individualMatch = match(FriendlyMatchSort.INDIVIDUAL, capacity, true, List.of(hostMember.id()));
        Long individualRequest = request(individualMatch, representative.id(), List.of(teammate.id()), MatchRequestState.PENDING);
        flushAndClear();
        approve(individualRequest, hostToken).andExpect(status().isOk());
        flushAndClear();
        assertThat(participants(individualRequest)).hasSize(2);
        assertThat(matchState(individualMatch)).isEqualTo(capacity == 3
                ? FriendlyMatchState.RECRUITMENT_CLOSED : FriendlyMatchState.RECRUITING);
    }

    // 동일 신청 재승인은 모집 종료 뒤에도 성공하지만 참가자를 중복 저장하지 않는다.
    @Test
    void 동일_신청을_재승인해도_참가자가_중복되지_않는다() throws Exception {
        approve(requestId, hostToken).andExpect(status().isOk());
        flushAndClear();
        approve(requestId, hostToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        flushAndClear();
        assertThat(participants(requestId)).containsExactly(representative.id(), teammate.id());
    }

    // 비주최자의 승인은 인증됐더라도 권한 오류이며 DB 상태를 바꾸지 않는다.
    @Test
    void 타인의_승인은_403이며_신청을_변경하지_않는다() throws Exception {
        String otherToken = tokenService.issueAccessToken(representative.userId()).value();
        approve(requestId, otherToken).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
        assertPendingUnchanged(requestId, matchId);
    }

    // 두 모집 방식 모두 정원을 초과하는 신청은 전체 승인을 거부한다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 정원을_초과하면_409이며_어느_참가자도_등록하지_않는다(FriendlyMatchSort sort) throws Exception {
        Long target = match(sort, sort == FriendlyMatchSort.TEAM ? 1 : 2, true, List.of(hostMember.id()));
        Long pending = request(target, representative.id(), List.of(teammate.id()), MatchRequestState.PENDING);
        flushAndClear();
        approve(pending, hostToken).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_004"));
        assertPendingUnchanged(pending, target);
    }

    // 미확정과 확정된 빈 명단을 구분한다.
    @Test
    void 주최_명단이_미확정이면_409이다() throws Exception {
        Long target = match(FriendlyMatchSort.TEAM, 5, false, List.of());
        Long pending = request(target, representative.id(), List.of(), MatchRequestState.PENDING);
        flushAndClear();
        approve(pending, hostToken).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_004"));
        assertPendingUnchanged(pending, target);
    }

    // 확정된 빈 명단에는 주최자를 숨겨서 자동 가산하지 않는다.
    @Test
    void 확정된_빈_주최_명단도_승인할_수_있다() throws Exception {
        Long target = match(FriendlyMatchSort.INDIVIDUAL, 1, true, List.of());
        Long pending = request(target, representative.id(), List.of(), MatchRequestState.PENDING);
        flushAndClear();
        approve(pending, hostToken).andExpect(status().isOk());
        flushAndClear();
        assertThat(participants(pending)).containsExactly(representative.id());
        assertThat(matchState(target)).isEqualTo(FriendlyMatchState.RECRUITMENT_CLOSED);
    }

    // 양측 명단에 같은 프로필이 있으면 정원 여유와 관계없이 충돌이다.
    @Test
    void 주최_명단과_겹치는_신청은_409이다() throws Exception {
        matchRepository.findById(matchId).orElseThrow().confirmHostRoster(List.of(teammate.id()));
        flushAndClear();
        approve(requestId, hostToken).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_004"));
        assertPendingUnchanged(requestId, matchId);
    }

    // TEAM은 첫 상대 승인 후 다른 상대 신청을 승인하지 않는다.
    @Test
    void 상대_팀이_승인된_뒤_다른_신청은_409이다() throws Exception {
        GameUserProfile other = profile("other");
        Long otherRequest = request(matchId, other.id(), List.of(), MatchRequestState.PENDING);
        approve(requestId, hostToken).andExpect(status().isOk());
        flushAndClear();
        approve(otherRequest, hostToken).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_003"));
        flushAndClear();
        assertThat(requestState(otherRequest)).isEqualTo(MatchRequestState.PENDING);
        assertThat(participants(otherRequest)).isEmpty();
    }

    // 취소나 거절된 신청은 승인 상태로 직접 전환하지 않는다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"CANCELLED", "REJECTED"})
    void 비활성_신청을_승인하면_409이다(MatchRequestState state) throws Exception {
        Long pending = request(matchId, profile("inactive").id(), List.of(), state);
        flushAndClear();
        approve(pending, hostToken).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_004"));
        flushAndClear();
        assertThat(requestState(pending)).isEqualTo(state);
        assertThat(participants(pending)).isEmpty();
    }

    // 실제 인증 필터가 토큰 없는 요청을 승인 처리 전에 거부한다.
    @Test
    void 인증이_없으면_401이며_DB를_변경하지_않는다() throws Exception {
        mockMvc.perform(patch("/match-requests/{id}/status", requestId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
        assertPendingUnchanged(requestId, matchId);
    }

    // 신청 미존재는 실제 조회를 거쳐 고유 코드로 반환한다.
    @Test
    void 신청이_없으면_404이다() throws Exception {
        approve(Long.MAX_VALUE, hostToken).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_001"));
        assertPendingUnchanged(requestId, matchId);
    }

    // 잘못된 상태 입력은 실제 Service에 도달하기 전에 400으로 거부한다.
    @Test
    void 거절_상태_입력은_400이다() throws Exception {
        mockMvc.perform(patch("/match-requests/{id}/status", requestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        assertPendingUnchanged(requestId, matchId);
    }

    // 승인 요청은 실제 JWT와 HTTP 본문으로 전송한다.
    private ResultActions approve(Long id, String token) throws Exception {
        return mockMvc.perform(patch("/match-requests/{id}/status", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"));
    }

    // 사용자 ID와 게임 프로필 ID가 서로 다른 실제 프로필을 만든다.
    private GameUserProfile profile(String nickname) {
        Long userId = userRepository.saveAndFlush(UserJpaEntity.create()).getId();
        Long profileId = profileRepository.saveAndFlush(GameUserJpaEntity.of(userId, game.id(), nickname)).getId();
        return new GameUserProfile(profileId, nickname, game, userId);
    }

    // 실제 주최 명단의 확정 여부를 포함한 경기 데이터를 만든다.
    private Long match(FriendlyMatchSort sort, int capacity, boolean confirmed, List<Long> hostIds) {
        FriendlyMatchJpaEntity entity = FriendlyMatchJpaEntity.from(new FriendlyMatch(null, host, game,
                LocalDate.of(2027, 6, 10), LocalTime.of(20, 0), sort,
                FriendlyMatchState.RECRUITING, capacity, "승인 HTTP 검증"));
        if (confirmed) entity.confirmHostRoster(hostIds);
        return matchRepository.saveAndFlush(entity).getId();
    }

    // 상태 전이 입력을 검증할 신청 행과 명단을 저장한다.
    private Long request(Long target, Long representativeId, List<Long> members, MatchRequestState state) {
        return requestRepository.saveAndFlush(MatchRequestJpaEntity.from(new MatchRequest(
                null, target, representativeId, members, "신청", state))).getId();
    }

    // 응답 후 영속성 컨텍스트를 비우고 실패 요청의 저장 결과가 없는지 확인한다.
    private void assertPendingUnchanged(Long id, Long target) {
        flushAndClear();
        assertThat(requestState(id)).isEqualTo(MatchRequestState.PENDING);
        assertThat(participants(id)).isEmpty();
        assertThat(matchState(target)).isEqualTo(FriendlyMatchState.RECRUITING);
    }

    // 실제 참가자 테이블을 신청별 명단 순서로 조회한다.
    private List<Long> participants(Long id) {
        return jdbcTemplate.queryForList("select game_user_id from friendly_match_participant where request_id=? order by member_order", Long.class, id);
    }

    // 주최 측 명단도 DB에 저장된 순서대로 확인한다.
    private List<Long> hostIds(Long id) {
        return jdbcTemplate.queryForList("select game_user_id from friendly_match_host_member where match_id=? order by member_order", Long.class, id);
    }

    // 캐시를 비운 뒤 신청 상태를 복원한다.
    private MatchRequestState requestState(Long id) {
        return requestRepository.findById(id).orElseThrow().getState();
    }

    // 캐시를 비운 뒤 경기 모집 상태를 복원한다.
    private FriendlyMatchState matchState(Long id) {
        return matchRepository.findById(id).orElseThrow().getState();
    }

    // SQL 반영 후 1차 캐시를 비워 실제 DB 반영 결과를 확인한다.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

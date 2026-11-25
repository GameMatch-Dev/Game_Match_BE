package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.friendlymatch.domain.MatchApprovalContext;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 한 트랜잭션의 저장과 복원을 검증한다. 실제 경합 대기와 독립 커밋 경계의 롤백은 후속에서 검증한다.
@DataJpaTest
@Import(MatchRequestApprovalPersistenceAdapter.class)
class MatchRequestApprovalPersistenceAdapterIntegrationTest {

    @Autowired
    private MatchRequestApprovalPersistenceAdapter adapter;
    @Autowired
    private FriendlyMatchJpaRepository matchRepository;
    @Autowired
    private MatchRequestJpaRepository requestRepository;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private GameJpaRepository gameRepository;
    @Autowired
    private GameUserJpaRepository profileRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Game game;
    private GameUserProfile host;
    private Long hostMemberId;
    private Long requesterId;
    private Long teammateId;
    private Long matchId;
    private Long requestId;

    // 실제 사용자와 게임 및 프로필을 만들고 대기 신청이 있는 TEAM 경기를 준비한다.
    @BeforeEach
    void 승인_대상_경기와_신청을_준비한다() {
        GameJpaEntity savedGame = gameRepository.saveAndFlush(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol"));
        game = Game.of(savedGame.getId(), savedGame.getName(), savedGame.getSort(), savedGame.getUrl());
        // 사용자 ID를 프로필 ID로 잘못 매핑하는 구현을 구분한다.
        userRepository.saveAndFlush(UserJpaEntity.create());
        host = saveProfile("host");
        hostMemberId = saveProfile("hostMember").id();
        requesterId = saveProfile("requester").id();
        teammateId = saveProfile("teammate").id();
        matchId = saveMatch(FriendlyMatchSort.TEAM, 5);
        requestId = saveRequest(matchId, requesterId, List.of(teammateId));
        flushAndClear();
    }

    // 존재하지 않는 신청은 경기 ID 조회 단계에서 빈 값으로 구분한다.
    @Test
    void 없는_신청의_경기_ID를_조회하면_빈_값이다() {
        assertThat(adapter.findMatchIdByRequestId(Long.MAX_VALUE)).isEmpty();
    }

    // 경기 잠금을 얻기 전에 필요한 식별자만 찾는다.
    @Test
    void 신청이_속한_경기_ID를_조회한다() {
        assertThat(adapter.findMatchIdByRequestId(requestId)).contains(matchId);
    }

    // 최신 신청 조회는 대표와 팀원 순서 및 현재 상태를 복원한다.
    @Test
    void 신청_내용과_팀원_순서를_복원한다() {
        // given: 대표와 구분되는 팀원 두 명을 역순으로 저장한다.
        Long anotherId = saveProfile("another").id();
        MatchRequestJpaEntity entity = requestRepository.findById(requestId).orElseThrow();
        entity.update(new MatchRequest(requestId, matchId, requesterId,
                List.of(anotherId, teammateId), "함께 참가합니다", MatchRequestState.PENDING));
        flushAndClear();

        // when / then: DB에서 읽은 신청의 값과 순서가 유지된다.
        assertThat(adapter.loadRequestById(requestId)).contains(new MatchRequest(
                requestId, matchId, requesterId, List.of(anotherId, teammateId),
                "함께 참가합니다", MatchRequestState.PENDING));
    }

    // 잠금 이후 신청이 삭제되었을 때에도 빈 값을 반환할 수 있어야 한다.
    @Test
    void 없는_신청을_다시_조회하면_빈_값이다() {
        assertThat(adapter.loadRequestById(Long.MAX_VALUE)).isEmpty();
    }

    // 없는 경기의 승인 판단 정보는 만들지 않는다.
    @Test
    void 없는_경기를_잠금_조회하면_빈_값이다() {
        assertThat(adapter.loadForUpdate(Long.MAX_VALUE)).isEmpty();
    }

    // 승인용 조회는 실제 주최 사용자와 경기 및 확정 명단을 복원한다.
    @Test
    void 승인_판단에_필요한_경기와_주최_명단을_복원한다() {
        // given: 관리자인 주최자는 참가 명단에 넣지 않는다.
        confirmHostRoster(matchId, List.of(hostMemberId));
        assertThat(host.userId()).isNotEqualTo(host.id());

        // when / then: 사용자 ID와 프로필 ID를 구분하고 상대가 없는 상태를 유지한다.
        assertThat(adapter.loadForUpdate(matchId)).contains(new MatchApprovalContext(
                matchId, host.userId(), game.id(), FriendlyMatchSort.TEAM,
                FriendlyMatchState.RECRUITING, 5, true, List.of(hostMemberId), List.of(), false));
    }

    // 빈 목록의 내용만으로 명단 확정 여부를 판단하지 않는다.
    @Test
    void 미확정_명단과_확정된_빈_명단을_구분한다() {
        // given / when: 처음에는 명단이 비어 있고 미확정이다.
        assertThat(adapter.loadForUpdate(matchId)).hasValueSatisfying(context -> {
            assertThat(context.hostRosterConfirmed()).isFalse();
            assertThat(context.hostTeamMemberIds()).isEmpty();
        });
        confirmHostRoster(matchId, List.of());

        // then: 같은 빈 목록이라도 명시적으로 확정한 상태를 복원한다.
        assertThat(adapter.loadForUpdate(matchId)).hasValueSatisfying(context -> {
            assertThat(context.hostRosterConfirmed()).isTrue();
            assertThat(context.hostTeamMemberIds()).isEmpty();
        });
    }

    // 저장 포트는 신청 승인과 참가자 행 및 모집 종료를 같은 트랜잭션에 반영한다.
    @Test
    void 팀전_승인_정보와_참가자_및_모집_종료를_저장한다() {
        // given: 주최 명단과 승인할 상대 명단을 별도로 준비한다.
        confirmHostRoster(matchId, List.of(hostMemberId));
        List<Long> participants = List.of(requesterId, teammateId);

        // when: 실제 저장 결과를 캐시가 아닌 DB에서 확인하도록 flush와 clear를 수행한다.
        adapter.loadForUpdate(matchId).orElseThrow();
        adapter.saveApproval(matchId, requestId, participants, true);
        flushAndClear();

        // then: 신청 내용은 유지하고 승인 상태와 해당 신청의 참가자만 저장한다.
        MatchRequestJpaEntity saved = requestRepository.findById(requestId).orElseThrow();
        assertThat(saved.getState()).isEqualTo(MatchRequestState.APPROVED);
        assertThat(saved.getTeamMemberIds()).containsExactly(teammateId);
        assertThat(saved.getComment()).isEqualTo("함께 참가합니다");
        assertThat(storedParticipants(requestId)).containsExactlyElementsOf(participants)
                .doesNotContain(host.id(), hostMemberId);
        assertThat(matchRepository.findById(matchId).orElseThrow().getState())
                .isEqualTo(FriendlyMatchState.RECRUITMENT_CLOSED);
        assertThat(jdbcTemplate.queryForList(
                "select game_user_id from friendly_match_host_member where match_id = ? order by member_order",
                Long.class, matchId)).containsExactly(hostMemberId);
    }

    // 개인전이 정원에 도달하지 않았으면 승인해도 모집은 계속한다.
    @Test
    void 모집_유지로_승인을_저장하면_개인전_모집_상태를_유지한다() {
        // given: 개인전 한 경기와 대기 신청을 추가한다.
        Long individualId = saveMatch(FriendlyMatchSort.INDIVIDUAL, 5);
        Long individualRequestId = saveRequest(individualId, requesterId, List.of(teammateId));
        confirmHostRoster(individualId, List.of());

        // when: Service가 결정한 모집 유지 값을 저장에 전달한다.
        adapter.loadForUpdate(individualId).orElseThrow();
        adapter.saveApproval(individualId, individualRequestId, List.of(requesterId, teammateId), false);
        flushAndClear();

        // then: 참가자와 승인 상태는 저장하고 모집 상태는 바꾸지 않는다.
        assertThat(requestRepository.findById(individualRequestId).orElseThrow().getState())
                .isEqualTo(MatchRequestState.APPROVED);
        assertThat(storedParticipants(individualRequestId)).containsExactly(requesterId, teammateId);
        assertThat(matchRepository.findById(individualId).orElseThrow().getState())
                .isEqualTo(FriendlyMatchState.RECRUITING);
    }

    // 승인 저장 결과를 다음 승인 판단에서도 읽을 수 있어야 한다.
    @Test
    void 승인된_참가자와_상대_팀_승인_여부를_다시_조회한다() {
        // given: TEAM 상대 신청을 승인 저장한다.
        confirmHostRoster(matchId, List.of(hostMemberId));
        adapter.loadForUpdate(matchId).orElseThrow();
        adapter.saveApproval(matchId, requestId, List.of(requesterId, teammateId), true);
        flushAndClear();

        // when / then: 상대 명단은 주최 명단과 섞지 않고 모집 종료도 함께 복원한다.
        assertThat(adapter.loadForUpdate(matchId)).hasValueSatisfying(context -> {
            assertThat(context.approvedParticipantIds()).containsExactlyInAnyOrder(requesterId, teammateId);
            assertThat(context.hostTeamMemberIds()).containsExactly(hostMemberId);
            assertThat(context.opponentTeamApproved()).isTrue();
            assertThat(context.state()).isEqualTo(FriendlyMatchState.RECRUITMENT_CLOSED);
        });
    }

    // 한 경기의 승인은 다른 경기와 대기 신청을 수정하지 않는다.
    @Test
    void 승인한_경기_외의_신청과_경기는_변경하지_않는다() {
        // given: 같은 경기의 다른 신청과 별도 경기의 신청을 준비한다.
        Long anotherRequester = saveProfile("otherRequester").id();
        Long pendingId = saveRequest(matchId, anotherRequester, List.of());
        Long otherMatchId = saveMatch(FriendlyMatchSort.TEAM, 5);
        Long otherRequestId = saveRequest(otherMatchId, requesterId, List.of());
        flushAndClear();

        // when: 첫 신청만 승인한다.
        adapter.loadForUpdate(matchId).orElseThrow();
        adapter.saveApproval(matchId, requestId, List.of(requesterId, teammateId), true);
        flushAndClear();

        // then: 나머지 신청은 대기 중이며 다른 경기에는 참가자 행이 없다.
        assertThat(requestRepository.findById(pendingId).orElseThrow().getState())
                .isEqualTo(MatchRequestState.PENDING);
        assertThat(requestRepository.findById(otherRequestId).orElseThrow().getState())
                .isEqualTo(MatchRequestState.PENDING);
        assertThat(matchRepository.findById(otherMatchId).orElseThrow().getState())
                .isEqualTo(FriendlyMatchState.RECRUITING);
        assertThat(storedParticipants(pendingId)).isEmpty();
        assertThat(storedParticipants(otherRequestId)).isEmpty();
    }

    // 사용자 소유의 실제 게임 프로필을 저장한다.
    private GameUserProfile saveProfile(String nickname) {
        Long userId = userRepository.saveAndFlush(UserJpaEntity.create()).getId();
        GameUserJpaEntity profile = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(userId, game.id(), nickname));
        return new GameUserProfile(profile.getId(), nickname, game, userId);
    }

    // 승인 판단에 필요한 모집 방식과 정원을 가진 경기 행을 만든다.
    private Long saveMatch(FriendlyMatchSort sort, int capacity) {
        return matchRepository.saveAndFlush(FriendlyMatchJpaEntity.from(new FriendlyMatch(
                null, host, game, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                sort, FriendlyMatchState.RECRUITING, capacity, "함께 경기해요"))).getId();
    }

    // 실제 대표와 팀원 순서를 가진 대기 신청 행을 만든다.
    private Long saveRequest(Long targetMatchId, Long representativeId, List<Long> memberIds) {
        return requestRepository.saveAndFlush(MatchRequestJpaEntity.from(new MatchRequest(
                null, targetMatchId, representativeId, memberIds, "함께 참가합니다",
                MatchRequestState.PENDING))).getId();
    }

    // 주최 측 명단 준비는 기존 Entity의 명단 확정 기능을 사용한다.
    private void confirmHostRoster(Long targetMatchId, List<Long> memberIds) {
        matchRepository.findById(targetMatchId).orElseThrow().confirmHostRoster(memberIds);
        flushAndClear();
    }

    // 신규 내부 저장 구조 제안이며 승인된 상대 명단만 신청별 순서대로 확인한다.
    private List<Long> storedParticipants(Long targetRequestId) {
        return jdbcTemplate.queryForList(
                "select game_user_id from friendly_match_participant where request_id = ? order by member_order",
                Long.class, targetRequestId);
    }

    // SQL 반영 후 영속성 컨텍스트를 비워 실제 DB 복원 결과를 검증한다.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

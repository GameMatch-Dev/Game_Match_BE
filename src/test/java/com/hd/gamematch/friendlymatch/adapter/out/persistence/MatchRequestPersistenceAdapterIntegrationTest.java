package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAlreadyActiveException;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.friendlymatch.domain.MatchRequestTarget;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 아직 없는 Adapter 구현보다 먼저 저장 및 조회의 DB 계약을 작성한다.
// 신청은 match_request 한 행, 팀원은 match_request_member의 member_order 순서로 저장한다.
@DataJpaTest
@Import(MatchRequestPersistenceAdapter.class)
class MatchRequestPersistenceAdapterIntegrationTest {

    @Autowired
    private MatchRequestPersistenceAdapter adapter;

    @Autowired
    private FriendlyMatchJpaRepository matchRepository;

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
    private Long requesterId;
    private Long firstMemberId;
    private Long secondMemberId;
    private Long thirdMemberId;
    private Long matchId;

    // 실제 사용자, 게임, 게임 프로필과 모집 중인 경기를 준비한다.
    @BeforeEach
    void 저장할_신청에_연결된_기본_데이터를_준비한다() {
        GameJpaEntity savedGame = gameRepository.saveAndFlush(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        game = Game.of(savedGame.getId(), savedGame.getName(), savedGame.getSort(), savedGame.getUrl());
        host = saveProfile("host");
        requesterId = saveProfile("requester").id();
        firstMemberId = saveProfile("memberA").id();
        secondMemberId = saveProfile("memberB").id();
        thirdMemberId = saveProfile("memberC").id();
        matchId = saveMatch();
        flushAndClear();
    }

    // 모집 중인 경기에서 신청 판단에 필요한 정보를 반환하는지 검증한다.
    @Test
    void 모집_중인_경기의_신청_대상_정보를_조회한다() {
        // 저장된 경기의 식별자, 게임 식별자와 모집 여부를 조회한다.
        assertThat(adapter.loadMatchRequestTarget(matchId))
                .contains(new MatchRequestTarget(matchId, game.id(), true));
    }

    // 대상 경기가 없으면 빈 조회 결과를 반환하는지 검증한다.
    @Test
    void 존재하지_않는_경기의_신청_대상은_빈_값이다() {
        // 실제 경기와 구별되는 미저장 식별자로 조회한다.
        assertThat(adapter.loadMatchRequestTarget(Long.MAX_VALUE)).isEmpty();
    }

    // 팀원 없는 신청의 ID 발급과 실제 DB 저장을 검증한다.
    @Test
    void 팀원_없는_새_신청을_저장하고_발급된_ID를_반환한다() {
        MatchRequest request = pendingRequest(null, List.of(), "혼자 신청합니다");

        // 저장 후 영속성 문맥을 비워 DB에 기록된 값을 직접 확인한다.
        Long savedId = adapter.save(request);
        flushAndClear();

        assertThat(savedId).isNotNull();
        assertStoredHeader(savedId, request);
        assertThat(storedMemberIds(savedId)).isEmpty();
        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, requesterId))
                .contains(new MatchRequest(savedId, matchId, requesterId, List.of(),
                        request.comment(), MatchRequestState.PENDING));
    }

    // 팀원 목록의 입력 순서와 신청 소개가 함께 저장되는지 검증한다.
    @Test
    void 팀원_있는_새_신청은_입력_순서와_소개_글을_저장한다() {
        // ID 오름차순과 다른 순서로 저장해 입력 목록의 순서 보존을 검증한다.
        List<Long> members = List.of(secondMemberId, firstMemberId);
        MatchRequest request = pendingRequest(null, members, "두 팀원과 신청합니다");

        Long savedId = adapter.save(request);
        flushAndClear();

        assertThat(savedId).isNotNull();
        assertStoredHeader(savedId, request);
        assertThat(storedMemberIds(savedId)).containsExactlyElementsOf(members);
        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, requesterId))
                .contains(new MatchRequest(savedId, matchId, requesterId, members,
                        request.comment(), MatchRequestState.PENDING));
    }

    // 네 가지 내부 상태와 팀원 목록을 DB에서 정확히 복원하는지 검증한다.
    @ParameterizedTest
    @EnumSource(MatchRequestState.class)
    void 저장된_모든_내부_상태와_팀원_목록을_조회한다(MatchRequestState state) {
        List<Long> members = List.of(secondMemberId, firstMemberId);
        // Adapter 저장 로직을 거치지 않은 DB 행으로 조회 매핑을 독립적으로 검증한다.
        Long storedId = insertRequest(matchId, requesterId, state, "저장된 소개", members);
        flushAndClear();

        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, requesterId))
                .contains(new MatchRequest(storedId, matchId, requesterId, members, "저장된 소개", state));
    }

    // 경기와 신청자에 일치하는 신청이 없으면 빈 결과인지 검증한다.
    @Test
    void 경기와_프로필에_해당하는_신청이_없으면_빈_값이다() {
        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, requesterId)).isEmpty();
    }

    // 경기와 신청자 조건을 함께 적용하고 다른 조합의 신청을 구별하는지 검증한다.
    @Test
    void 경기와_신청자_조건을_모두_사용하고_서로_다른_조합은_허용한다() {
        Long anotherMatchId = saveMatch();
        // 같은 경기의 다른 신청과 같은 신청자의 다른 경기 신청을 각각 저장한다.
        Long otherRequesterRequestId = insertRequest(matchId, firstMemberId,
                MatchRequestState.PENDING, "다른 신청자", List.of(secondMemberId));
        Long otherMatchRequestId = insertRequest(anotherMatchId, requesterId,
                MatchRequestState.PENDING, "다른 경기", List.of(thirdMemberId));
        flushAndClear();

        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, requesterId)).isEmpty();
        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, firstMemberId))
                .contains(new MatchRequest(otherRequesterRequestId, matchId, firstMemberId,
                        List.of(secondMemberId), "다른 신청자", MatchRequestState.PENDING));
        assertThat(adapter.loadByMatchIdAndGameUserId(anotherMatchId, requesterId))
                .contains(new MatchRequest(otherMatchRequestId, anotherMatchId, requesterId,
                        List.of(thirdMemberId), "다른 경기", MatchRequestState.PENDING));
    }

    // 취소되거나 거절된 신청의 ID를 유지하며 새 내용으로 갱신하는지 검증한다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"CANCELLED", "REJECTED"})
    void 재신청은_기존_ID를_유지하고_팀원과_소개를_교체한다(MatchRequestState previousState) {
        Long existingId = insertRequest(matchId, requesterId, previousState, "이전 소개",
                List.of(firstMemberId, secondMemberId));
        flushAndClear();
        List<Long> newMembers = List.of(thirdMemberId, firstMemberId);
        MatchRequest reapplication = pendingRequest(existingId, newMembers, "다시 신청합니다");

        // 기존 신청의 ID로 저장하므로 별도의 신청 행을 추가하지 않는다.
        Long savedId = adapter.save(reapplication);
        flushAndClear();

        assertThat(savedId).isEqualTo(existingId);
        assertThat(requestCount()).isEqualTo(1L);
        assertStoredHeader(existingId, reapplication);
        assertThat(storedMemberIds(existingId)).containsExactlyElementsOf(newMembers);
        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, requesterId)).contains(reapplication);
    }

    // 재신청의 팀원 목록이 비어 있으면 이전 팀원 행이 모두 사라지는지 검증한다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"CANCELLED", "REJECTED"})
    void 팀원_없이_재신청하면_기존_팀원_행을_모두_삭제한다(MatchRequestState previousState) {
        Long existingId = insertRequest(matchId, requesterId, previousState, "이전 소개",
                List.of(firstMemberId, secondMemberId));
        flushAndClear();
        MatchRequest reapplication = pendingRequest(existingId, List.of(), "혼자 다시 신청합니다");

        Long savedId = adapter.save(reapplication);
        flushAndClear();

        assertThat(savedId).isEqualTo(existingId);
        assertThat(requestCount()).isEqualTo(1L);
        assertStoredHeader(existingId, reapplication);
        assertThat(storedMemberIds(existingId)).isEmpty();
        assertThat(adapter.loadByMatchIdAndGameUserId(matchId, requesterId)).contains(reapplication);
    }

    // 사전 조회 없이 중복 행을 추가해도 DB 유니크 제약이 거부하는지 검증한다.
    @Test
    void 같은_경기와_신청자_조합의_새_행은_DB가_거부한다() {
        insertRequest(matchId, requesterId, MatchRequestState.PENDING, "기존 신청", List.of());
        flushAndClear();

        // 서비스 사전 조회를 우회한 INSERT도 DB의 복합 유니크 제약이 막아야 한다.
        // 이 검증은 동시 트랜잭션 실행 자체의 증거는 아니다.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into match_request (match_id, game_user_id, state, comment) values (?, ?, ?, ?)",
                matchId, requesterId, MatchRequestState.PENDING.name(), "중복 신청"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Adapter 저장 중 발생한 경기와 신청자 중복을 업무 예외로 변환하는지 검증한다.
    @Test
    void 새_신청의_경기와_신청자_중복을_업무_예외로_변환한다() {
        insertRequest(matchId, requesterId, MatchRequestState.PENDING, "기존 신청", List.of());
        flushAndClear();

        // Adapter는 자신이 실행한 저장의 중복 제약 위반을 업무 예외로 변환한다.
        assertThatThrownBy(() -> adapter.save(pendingRequest(null, List.of(), "중복 신청")))
                .isInstanceOf(MatchRequestAlreadyActiveException.class);
    }

    // 필수 식별자 누락을 중복 신청으로 잘못 분류하지 않는지 검증한다.
    @Test
    void 경기_식별자_누락은_신청_중복_업무_예외로_변환하지_않는다() {
        MatchRequest missingMatchId = new MatchRequest(null, null, requesterId, List.of(),
                "필수 식별자 누락", MatchRequestState.PENDING);

        // match_id의 NOT NULL 위반과 복합 유니크 제약 위반을 구별한다.
        assertThatThrownBy(() -> {
            adapter.save(missingMatchId);
            entityManager.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(MatchRequestAlreadyActiveException.class);
    }

    // 실제 사용자와 연결된 게임 프로필을 만들어 식별자와 도메인 값을 반환한다.
    private GameUserProfile saveProfile(String nickname) {
        UserJpaEntity user = userRepository.saveAndFlush(UserJpaEntity.create());
        GameUserJpaEntity profile = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(user.getId(), game.id(), nickname)
        );
        return new GameUserProfile(profile.getId(), nickname, game, user.getId());
    }

    // 기존 친선 경기 Entity를 사용해 현재 도메인이 제공하는 모집 중 상태로 저장한다.
    private Long saveMatch() {
        FriendlyMatch match = new FriendlyMatch(null, host, game, LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0), FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING,
                5, "함께 경기해요");
        return matchRepository.saveAndFlush(FriendlyMatchJpaEntity.from(match)).getId();
    }

    // 현재 fixture의 경기와 신청자로 새 신청 또는 재신청 저장 입력을 만든다.
    private MatchRequest pendingRequest(Long id, List<Long> members, String comment) {
        return new MatchRequest(id, matchId, requesterId, members, comment, MatchRequestState.PENDING);
    }

    // 아직 구현되지 않은 신청 Entity에 의존하지 않고 조회용 DB 행을 준비한다.
    private Long insertRequest(Long targetMatchId, Long gameUserId, MatchRequestState state,
                               String comment, List<Long> members) {
        jdbcTemplate.update(
                "insert into match_request (match_id, game_user_id, state, comment) values (?, ?, ?, ?)",
                targetMatchId, gameUserId, state.name(), comment
        );
        Long requestId = jdbcTemplate.queryForObject(
                "select id from match_request where match_id = ? and game_user_id = ?",
                Long.class, targetMatchId, gameUserId
        );
        for (int order = 0; order < members.size(); order++) {
            jdbcTemplate.update(
                    "insert into match_request_member (request_id, game_user_id, member_order) values (?, ?, ?)",
                    requestId, members.get(order), order
            );
        }
        return requestId;
    }

    // DB 행을 도메인 변환 없이 읽어 저장 컬럼의 매핑을 확인한다.
    private void assertStoredHeader(Long id, MatchRequest expected) {
        MatchRequest header = jdbcTemplate.queryForObject(
                "select id, match_id, game_user_id, state, comment from match_request where id = ?",
                (row, rowNumber) -> new MatchRequest(row.getLong("id"), row.getLong("match_id"),
                        row.getLong("game_user_id"), List.of(), row.getString("comment"),
                        MatchRequestState.valueOf(row.getString("state"))), id
        );
        assertThat(header).isEqualTo(new MatchRequest(id, expected.matchId(), expected.gameUserId(),
                List.of(), expected.comment(), expected.state()));
    }

    // 팀원 테이블의 실제 행을 저장된 순서로 확인한다.
    private List<Long> storedMemberIds(Long requestId) {
        return jdbcTemplate.queryForList(
                "select game_user_id from match_request_member where request_id = ? order by member_order",
                Long.class, requestId
        );
    }

    // 재신청 시 신청 행이 추가되지 않았는지 확인한다.
    private Long requestCount() {
        return jdbcTemplate.queryForObject("select count(*) from match_request", Long.class);
    }

    // 지연된 SQL을 실행하고 영속성 문맥의 캐시를 제거한다.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

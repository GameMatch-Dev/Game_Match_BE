package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAlreadyActiveException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 H2 트랜잭션의 재신청 경쟁을 검증하며 MySQL 실행 특성을 증명하지는 않는다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:request-reactivation-race;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class MatchRequestReactivationConcurrencyIntegrationTest {

    @Autowired private MatchRequestPersistenceAdapter adapter;
    @Autowired private FriendlyMatchJpaRepository matchRepository;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private GameJpaRepository gameRepository;
    @Autowired private GameUserJpaRepository profileRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> profileIds = new ArrayList<>();
    private TransactionTemplate transactions;
    private Game game;
    private Long matchId;
    private Long requesterId;
    private Long requestId;

    // 작업 스레드가 조회할 수 있도록 이 테스트 소유의 fixture를 먼저 커밋한다.
    @BeforeEach
    void 취소된_빈_팀_신청을_커밋한다() {
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            GameJpaEntity savedGame = gameRepository.saveAndFlush(
                    GameJpaEntity.of("Reactivation race", "MOBA", "https://example.com/race"));
            game = Game.of(savedGame.getId(), savedGame.getName(), savedGame.getSort(), savedGame.getUrl());
            GameUserProfile host = saveProfile("race-host");
            requesterId = saveProfile("race-requester").id();
            FriendlyMatch match = new FriendlyMatch(null, host, game, LocalDate.of(2027, 6, 10),
                    LocalTime.of(20, 0), FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING,
                    5, "동시 재신청 검증");
            matchId = matchRepository.saveAndFlush(FriendlyMatchJpaEntity.from(match)).getId();
            jdbcTemplate.update(
                    "insert into match_request (match_id, game_user_id, state, comment) values (?, ?, ?, ?)",
                    matchId, requesterId, MatchRequestState.CANCELLED.name(), "이전 신청");
            requestId = jdbcTemplate.queryForObject(
                    "select id from match_request where match_id = ? and game_user_id = ?",
                    Long.class, matchId, requesterId);
        });
    }

    // 두 트랜잭션이 같은 취소 상태를 읽은 뒤 재신청해도 한 건만 성공해야 한다.
    @Test
    void 같은_취소_신청의_동시_재신청은_하나만_성공한다() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<Outcome> first = workers.submit(() -> reactivate("첫 번째 재신청", ready, release));
            Future<Outcome> second = workers.submit(() -> reactivate("두 번째 재신청", ready, release));
            // 두 영속성 문맥이 모두 이전 상태를 보유한 뒤 저장을 동시에 허용한다.
            assertThat(ready.await(10, TimeUnit.SECONDS)).as("두 트랜잭션의 취소 상태 조회").isTrue();
            release.countDown();
            List<Outcome> outcomes = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

            assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
            assertThat(outcomes).filteredOn(outcome -> !outcome.success()).hasSize(1);
            String winnerComment = outcomes.stream().filter(Outcome::success)
                    .findFirst().orElseThrow().comment();
            // 캐시를 거치지 않고 원래 행, 최종 상태, 승자의 소개와 빈 팀을 확인한다.
            assertThat(jdbcTemplate.queryForList(
                    "select id from match_request where match_id = ? and game_user_id = ?",
                    Long.class, matchId, requesterId)).containsExactly(requestId);
            assertThat(jdbcTemplate.queryForObject("select state from match_request where id = ?",
                    String.class, requestId)).isEqualTo(MatchRequestState.PENDING.name());
            assertThat(jdbcTemplate.queryForObject("select comment from match_request where id = ?",
                    String.class, requestId)).isEqualTo(winnerComment);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from match_request_member where request_id = ?",
                    Long.class, requestId)).isZero();
        } finally {
            // 준비 대기나 검증 실패 시에도 스레드를 해제하고 종료 시간을 제한한다.
            release.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).as("작업 스레드 종료").isTrue();
        }
    }

    // 각 작업은 독립 트랜잭션에서 읽고 저장하며 업무 충돌만 정상 결과로 구분한다.
    private Outcome reactivate(String comment, CountDownLatch ready, CountDownLatch release) {
        try {
            transactions.executeWithoutResult(status -> {
                MatchRequest previous = adapter.loadByMatchIdAndGameUserId(matchId, requesterId).orElseThrow();
                assertThat(previous.state()).isEqualTo(MatchRequestState.CANCELLED);
                ready.countDown();
                try {
                    if (!release.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("재신청 시작 대기 시간 초과");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재신청 작업 중단", exception);
                }
                adapter.save(new MatchRequest(previous.id(), matchId, requesterId, List.of(),
                        comment, MatchRequestState.PENDING));
            });
            return new Outcome(true, comment);
        } catch (MatchRequestAlreadyActiveException exception) {
            return new Outcome(false, comment);
        }
    }

    // 생성한 사용자와 프로필의 실제 ID를 보관해 정리 범위를 제한한다.
    private GameUserProfile saveProfile(String nickname) {
        Long userId = userRepository.saveAndFlush(UserJpaEntity.create()).getId();
        userIds.add(userId);
        GameUserJpaEntity profile = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(userId, game.id(), nickname));
        profileIds.add(profile.getId());
        return new GameUserProfile(profile.getId(), nickname, game, userId);
    }

    // 테스트가 만든 정확한 ID만 외래 키의 역순으로 삭제한다.
    @AfterEach
    void 소유한_fixture만_정리한다() {
        transactions.executeWithoutResult(status -> {
            jdbcTemplate.update("delete from match_request_member where request_id = ?", requestId);
            jdbcTemplate.update("delete from match_request where id = ?", requestId);
            jdbcTemplate.update("delete from friendly_match where id = ?", matchId);
            profileIds.forEach(id -> jdbcTemplate.update("delete from game_user where id = ?", id));
            userIds.forEach(id -> jdbcTemplate.update("delete from users where id = ?", id));
            if (game != null) {
                jdbcTemplate.update("delete from game where id = ?", game.id());
            }
        });
    }

    // 성공 여부와 해당 작업의 소개를 보관해 실제 승자의 저장 내용을 비교한다.
    private record Outcome(boolean success, String comment) { }
}

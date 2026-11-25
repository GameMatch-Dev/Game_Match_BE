package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.adapter.out.persistence.MatchRequestApprovalPersistenceAdapter;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.application.port.out.SaveMatchRequestApprovalPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// H2에서 실제 Service의 독립 커밋과 저장 SQL 실행 이후의 롤백을 검증한다.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:approval_transaction;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@Import(MatchRequestApprovalTransactionIntegrationTest.FailureConfiguration.class)
class MatchRequestApprovalTransactionIntegrationTest extends ApprovalCommittedTestSupport {
    @Autowired ApproveMatchRequestUseCase approval;
    @Autowired FlushThenFailStore store;

    // 테스트 트랜잭션 없이 호출한 Service의 정상 결과는 다음 조회에서도 남아 있어야 한다.
    @Test
    void 정상_승인은_Service_트랜잭션이_끝난_뒤에도_저장된다() {
        // given: 데이터 준비를 먼저 커밋한다.
        Long[] ids = inNewTransaction(() -> {
            Long matchId = newMatch(FriendlyMatchSort.TEAM, 5);
            return new Long[]{matchId, newRequest(matchId)};
        });
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

        // when: 프록시가 생성한 Service 트랜잭션에서 승인하고 커밋한다.
        approval.approve(ApproveMatchRequestCommand.of(host.userId(), ids[1]));

        // then: 새 트랜잭션에서 참가자와 두 상태의 커밋을 확인한다.
        inNewTransaction(() -> {
            assertThat(participantCount(ids[0])).isEqualTo(1);
            assertThat(requestState(ids[1])).isEqualTo("APPROVED");
            assertThat(matchState(ids[0])).isEqualTo("RECRUITMENT_CLOSED");
            return null;
        });
    }

    // 세 변경의 SQL이 실제 실행된 직후 예외가 나면 Service 트랜잭션 전체가 원복돼야 한다.
    @Test
    void 실제_저장_SQL_이후_실패하면_참가자와_신청과_경기_상태가_함께_롤백된다() {
        Long[] ids = inNewTransaction(() -> {
            Long matchId = newMatch(FriendlyMatchSort.TEAM, 5);
            return new Long[]{matchId, newRequest(matchId)};
        });
        store.failNext.set(true);

        // when: 테스트 전용 저장 장식자가 flush 후 예외를 던진다.
        assertThatThrownBy(() -> approval.approve(ApproveMatchRequestCommand.of(host.userId(), ids[1])))
                .isInstanceOf(InjectedSaveFailure.class);
        assertThat(store.flushedChangesObserved).isTrue();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

        // then: 실패한 트랜잭션 밖의 새 조회에서는 모든 변경이 사라져야 한다.
        inNewTransaction(() -> {
            assertThat(participantCount(ids[0])).isZero();
            assertThat(requestState(ids[1])).isEqualTo("PENDING");
            assertThat(matchState(ids[0])).isEqualTo("RECRUITING");
            return null;
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureConfiguration {
        // 실제 Adapter는 그대로 호출하고 실패 주입은 이 테스트 컨텍스트에만 둔다.
        @Bean
        @Primary
        FlushThenFailStore flushThenFailStore(MatchRequestApprovalPersistenceAdapter adapter,
                                              EntityManager entityManager, JdbcTemplate jdbc) {
            return new FlushThenFailStore(adapter, entityManager, jdbc);
        }
    }

    static class FlushThenFailStore implements SaveMatchRequestApprovalPort {
        private final MatchRequestApprovalPersistenceAdapter adapter;
        private final EntityManager entityManager;
        private final JdbcTemplate jdbc;
        final AtomicBoolean failNext = new AtomicBoolean();
        volatile boolean flushedChangesObserved;

        FlushThenFailStore(MatchRequestApprovalPersistenceAdapter adapter, EntityManager entityManager,
                           JdbcTemplate jdbc) {
            this.adapter = adapter;
            this.entityManager = entityManager;
            this.jdbc = jdbc;
        }

        // 메모리 변경만이 아니라 DB에 반영된 세 SQL 결과를 관찰한 다음 실패시킨다.
        @Override
        public void saveApproval(Long matchId, Long requestId, List<Long> participantIds, boolean closeRecruitment) {
            adapter.saveApproval(matchId, requestId, participantIds, closeRecruitment);
            if (failNext.getAndSet(false)) {
                entityManager.flush();
                flushedChangesObserved = participantIds.size() == jdbc.queryForObject(
                        "select count(*) from friendly_match_participant where request_id=?", Integer.class, requestId)
                        && "APPROVED".equals(jdbc.queryForObject(
                        "select state from match_request where id=?", String.class, requestId))
                        && "RECRUITMENT_CLOSED".equals(jdbc.queryForObject(
                        "select state from friendly_match where id=?", String.class, matchId));
                throw new InjectedSaveFailure();
            }
        }
    }

    static class InjectedSaveFailure extends RuntimeException {
    }
}

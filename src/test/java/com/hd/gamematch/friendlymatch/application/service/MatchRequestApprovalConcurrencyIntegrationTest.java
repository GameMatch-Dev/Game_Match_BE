package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// H2의 독립 HTTP 요청과 트랜잭션을 검증한다. MySQL 성능과 요청 선착순을 증명하는 테스트는 아니다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval_concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.hikari.maximum-pool-size=12"
})
@AutoConfigureMockMvc
class MatchRequestApprovalConcurrencyIntegrationTest extends ApprovalCommittedTestSupport {
    @Autowired MockMvc mockMvc;
    @Autowired ApproveMatchRequestUseCase approval;
    @Autowired EntityManager entityManager;

    // 마지막 한 자리에 HTTP 요청 10건을 동시에 시작해 실제 응답과 커밋된 정원을 확인한다.
    @Test
    void 마지막_한_자리에_동시_승인_10건이_들어와도_한_건만_200이다() throws Exception {
        List<Long> requestIds = new ArrayList<>();
        Long matchId = inNewTransaction(() -> {
            Long id = newMatch(FriendlyMatchSort.INDIVIDUAL, 2);
            for (int i = 0; i < 10; i++) requestIds.add(newRequest(id));
            return id;
        });
        String token = tokens.issueAccessToken(host.userId()).value();

        // when: 각 작업은 별도 요청 스레드에서 실제 Security와 Service를 통과한다.
        List<HttpOutcome> outcomes = approveTogether(requestIds, token);

        // then: 나머지 9건은 서버 오류가 아닌 업무 충돌이고 참가자는 한 명만 추가된다.
        assertThat(outcomes).filteredOn(result -> result.status() == 200).hasSize(1);
        assertThat(outcomes).filteredOn(result -> result.status() == 409).hasSize(9);
        assertThat(outcomes).allSatisfy(result -> {
            if (result.status() == 409) assertThat(result.body()).containsAnyOf("MATCH_REQUEST_003", "MATCH_REQUEST_004");
        });
        inNewTransaction(() -> {
            assertThat(participantCount(matchId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from friendly_match_host_member where match_id=?", Integer.class, matchId)
                    + participantCount(matchId)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from match_request where match_id=? and state='APPROVED'", Integer.class, matchId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from match_request where match_id=? and state='PENDING'", Integer.class, matchId)).isEqualTo(9);
            assertThat(matchState(matchId)).isEqualTo("RECRUITMENT_CLOSED");
            return null;
        });
    }

    // 같은 신청의 동시 재호출은 모두 성공하되 실제 참가자는 한 번만 저장한다.
    @Test
    void 같은_신청을_10번_동시에_승인해도_참가자는_한_번만_저장된다() throws Exception {
        Long[] ids = inNewTransaction(() -> {
            Long id = newMatch(FriendlyMatchSort.TEAM, 5);
            return new Long[]{id, newRequest(id)};
        });
        String token = tokens.issueAccessToken(host.userId()).value();

        List<HttpOutcome> outcomes = approveTogether(java.util.Collections.nCopies(10, ids[1]), token);

        assertThat(outcomes).extracting(HttpOutcome::status).containsOnly(200);
        inNewTransaction(() -> {
            assertThat(participantCount(ids[0])).isEqualTo(1);
            assertThat(requestState(ids[1])).isEqualTo("APPROVED");
            assertThat(matchState(ids[0])).isEqualTo("RECRUITMENT_CLOSED");
            return null;
        });
    }

    // 승인 커밋을 미룬 동안 명단 PUT이 같은 경기 잠금에서 실제 대기하는지 관찰한다.
    @Test
    void 승인_트랜잭션이_끝난_뒤_대기하던_명단_변경은_409이다() throws Exception {
        Long[] ids = inNewTransaction(() -> {
            Long id = newMatch(FriendlyMatchSort.INDIVIDUAL, 3);
            return new Long[]{id, newRequest(id)};
        });
        String token = tokens.issueAccessToken(host.userId()).value();
        CountDownLatch approvalStored = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        AtomicLong blockingSession = new AtomicLong();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        Future<?> holder = null;
        Future<HttpOutcome> editor = null;
        try {
            // 승인 SQL을 실행한 후에도 트랜잭션을 끝내지 않아 경기 잠금을 유지한다.
            holder = workers.submit(() -> inNewTransaction(() -> {
                approval.approve(ApproveMatchRequestCommand.of(host.userId(), ids[1]));
                entityManager.flush();
                blockingSession.set(jdbc.queryForObject("select session_id()", Long.class));
                approvalStored.countDown();
                await(allowCommit);
                return null;
            }));
            assertThat(approvalStored.await(10, TimeUnit.SECONDS)).isTrue();
            editor = workers.submit(() -> {
                var response = mockMvc.perform(put("/friendly-matches/{id}/host-roster", ids[0])
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"teamMemberIds\":[]}"))
                        .andReturn().getResponse();
                return new HttpOutcome(response.getStatus(), response.getContentAsString());
            });

            // 단순 시간 지연 대신 H2에서 해당 승인 세션에 막힌 세션을 직접 확인한다.
            assertBlockedBy(blockingSession.get());
            assertThat(editor.isDone()).isFalse();
            allowCommit.countDown();
            holder.get(15, TimeUnit.SECONDS);
            HttpOutcome result = editor.get(15, TimeUnit.SECONDS);
            assertThat(result.status()).isEqualTo(409);
            assertThat(result.body()).contains("FRIENDLY_MATCH_002");

            inNewTransaction(() -> {
                assertThat(jdbc.queryForList("select game_user_id from friendly_match_host_member where match_id=? order by member_order", Long.class, ids[0]))
                        .containsExactly(hostMember.id());
                assertThat(requestState(ids[1])).isEqualTo("APPROVED");
                assertThat(participantCount(ids[0])).isEqualTo(1);
                assertThat(matchState(ids[0])).isEqualTo("RECRUITING");
                return null;
            });
        } finally {
            allowCommit.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    // 준비된 모든 작업이 출발선에 도착한 뒤 실제 HTTP 승인을 함께 시작한다.
    private List<HttpOutcome> approveTogether(List<Long> ids, String token) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(ids.size());
        CountDownLatch ready = new CountDownLatch(ids.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpOutcome>> futures = new ArrayList<>();
        try {
            for (Long id : ids) futures.add(workers.submit(() -> {
                ready.countDown();
                await(start);
                var response = mockMvc.perform(patch("/match-requests/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                        .andReturn().getResponse();
                return new HttpOutcome(response.getStatus(), response.getContentAsString());
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<HttpOutcome> results = new ArrayList<>();
            for (Future<HttpOutcome> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    // 테스트가 무한 대기하지 않도록 모든 동기화에 제한 시간을 둔다.
    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("트랜잭션 동기화 시간 초과");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("트랜잭션 동기화 중단", exception);
        }
    }

    // 대상 세션의 잠금 대기를 최대 5초 동안 관찰한다.
    private void assertBlockedBy(long sessionId) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            Integer blocked = jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id=?", Integer.class, sessionId);
            if (blocked != null && blocked > 0) return;
            Thread.sleep(20);
        }
        throw new AssertionError("승인 트랜잭션의 경기 잠금에서 대기하는 세션을 찾지 못했습니다.");
    }

    private record HttpOutcome(int status, String body) {
    }
}

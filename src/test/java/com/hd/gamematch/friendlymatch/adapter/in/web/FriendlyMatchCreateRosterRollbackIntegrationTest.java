package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.FriendlyMatchPersistenceAdapter;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// 테스트 자체의 트랜잭션 없이 서비스 트랜잭션의 실제 H2 롤백을 검증한다.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:friendly-match-create-roster-rollback;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class FriendlyMatchCreateRosterRollbackIntegrationTest {

    private static final String SAVE_FAILURE = "test-only failure after create roster flush";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private GameJpaRepository gameRepository;
    @Autowired private GameUserJpaRepository profileRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private FriendlyMatchPersistenceAdapter adapter;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> profileIds = new ArrayList<>();
    private final AtomicReference<Long> failedMatchId = new AtomicReference<>();
    private TransactionTemplate transactions;
    private Long gameId;
    private Long hostUserId;
    private Long hostProfileId;
    private Long memberProfileId;

    // 요청 전에 독립 트랜잭션으로 이 테스트 소유의 실제 사용자와 프로필을 커밋한다.
    @BeforeEach
    void 요청에_필요한_fixture를_커밋한다() {
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            gameId = gameRepository.saveAndFlush(
                    GameJpaEntity.of("Create rollback", "MOBA", "https://example.com/rollback")).getId();
            hostProfileId = saveProfile("rollback-host");
            hostUserId = userIds.get(0);
            memberProfileId = saveProfile("rollback-member");
        });
    }

    // 실제 명단 행을 flush한 직후 실패시키면 새 경기와 명단이 모두 롤백되어야 한다.
    @Test
    void 명단_저장_직후_실패하면_생성된_경기와_명단을_함께_롤백한다() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        Long matchesBefore = jdbcTemplate.queryForObject("select count(*) from friendly_match", Long.class);
        Long membersBefore = jdbcTemplate.queryForObject("select count(*) from friendly_match_host_member", Long.class);
        List<Long> requestedMembers = List.of(memberProfileId, hostProfileId);
        doAnswer(invocation -> {
            Long matchId = invocation.getArgument(0);
            failedMatchId.set(matchId);
            invocation.callRealMethod();
            // 실제 서비스 트랜잭션 안에서 새 경기와 정확한 명단 행이 저장되었음을 먼저 증명한다.
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            entityManager.flush();
            assertThat(jdbcTemplate.queryForObject("select count(*) from friendly_match where id = ?",
                    Long.class, matchId)).isEqualTo(1L);
            assertThat(jdbcTemplate.queryForObject("select host_roster_confirmed from friendly_match where id = ?",
                    Boolean.class, matchId)).isTrue();
            assertThat(jdbcTemplate.queryForList(
                    "select game_user_id from friendly_match_host_member where match_id = ? order by member_order",
                    Long.class, matchId)).containsExactlyElementsOf(requestedMembers);
            throw new IllegalStateException(SAVE_FAILURE);
        }).when(adapter).saveHostRoster(anyLong(), anyList());
        String token = jwtTokenService.issueAccessToken(hostUserId).value();

        // 별도 예외 매핑을 가정하지 않고 MockMvc가 전달하는 실제 실패 원인을 확인한다.
        assertThatThrownBy(() -> mockMvc.perform(post("/friendly-matches")
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
                                  "comment": "생성 트랜잭션 롤백 검증",
                                  "hostTeamMemberIds": [%d, %d]
                                }
                                """.formatted(hostProfileId, gameId, LocalDate.now().plusDays(1),
                                memberProfileId, hostProfileId))))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage(SAVE_FAILURE);

        // HTTP 호출이 끝난 뒤 별도 연결로 실패한 ID와 전체 행 수가 원상 복구되었는지 확인한다.
        assertThat(failedMatchId.get()).isNotNull();
        verify(adapter, times(1)).saveHostRoster(failedMatchId.get(), requestedMembers);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(jdbcTemplate.queryForObject("select count(*) from friendly_match where id = ?",
                Long.class, failedMatchId.get())).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from friendly_match_host_member where match_id = ?",
                Long.class, failedMatchId.get())).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from friendly_match", Long.class))
                .isEqualTo(matchesBefore);
        assertThat(jdbcTemplate.queryForObject("select count(*) from friendly_match_host_member", Long.class))
                .isEqualTo(membersBefore);
    }

    // 사용자와 프로필의 ID를 기록해 검증 실패 때도 정리 범위를 이 fixture로 제한한다.
    private Long saveProfile(String nickname) {
        Long userId = userRepository.saveAndFlush(UserJpaEntity.create()).getId();
        userIds.add(userId);
        Long profileId = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(userId, gameId, nickname)).getId();
        profileIds.add(profileId);
        return profileId;
    }

    // 롤백 검증이 실패해도 캡처한 경기 ID와 이 테스트가 만든 fixture만 외래 키 역순으로 정리한다.
    @AfterEach
    void 이_테스트가_생성한_행만_정리한다() {
        transactions.executeWithoutResult(status -> {
            if (failedMatchId.get() != null) {
                jdbcTemplate.update("delete from friendly_match_host_member where match_id = ?", failedMatchId.get());
                jdbcTemplate.update("delete from friendly_match where id = ?", failedMatchId.get());
            }
            profileIds.forEach(id -> jdbcTemplate.update("delete from game_user where id = ?", id));
            userIds.forEach(id -> jdbcTemplate.update("delete from users where id = ?", id));
            if (gameId != null) {
                jdbcTemplate.update("delete from game where id = ?", gameId);
            }
        });
    }
}

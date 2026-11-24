package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(FriendlyMatchPersistenceAdapter.class)
class FriendlyMatchHostRosterPersistenceAdapterIntegrationTest {

    @Autowired
    private FriendlyMatchPersistenceAdapter adapter;

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
    private Long firstMemberId;
    private Long secondMemberId;
    private Long thirdMemberId;
    private FriendlyMatch expectedMatch;

    // 실제 사용자와 게임 프로필을 저장하고 경기 조회의 기준값을 준비한다.
    @BeforeEach
    void 호스트_명단을_저장할_경기와_프로필을_준비한다() {
        GameJpaEntity savedGame = gameRepository.saveAndFlush(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        game = Game.of(savedGame.getId(), savedGame.getName(), savedGame.getSort(), savedGame.getUrl());
        // 사용자 ID와 프로필 ID를 구분해 잘못된 호스트 매핑을 검출한다.
        userRepository.saveAndFlush(UserJpaEntity.create());
        host = saveProfile("host");
        firstMemberId = saveProfile("memberA").id();
        secondMemberId = saveProfile("memberB").id();
        thirdMemberId = saveProfile("memberC").id();
        expectedMatch = saveMatch("함께 경기해요");
        flushAndClear();
    }

    // 캐시를 비운 뒤 호스트 사용자와 게임을 포함한 경기 전체 값을 복원한다.
    @Test
    void 저장된_경기의_호스트와_게임과_팀별_정원을_조회한다() {
        assertThat(host.userId()).isNotEqualTo(host.id());
        assertThat(adapter.loadFriendlyMatch(expectedMatch.id())).contains(expectedMatch);
        assertThat(storedConfirmed(expectedMatch.id())).isFalse();
    }

    // 저장되지 않은 경기 식별자로 조회하면 빈 결과를 반환한다.
    @Test
    void 존재하지_않는_경기를_조회하면_빈_값이다() {
        assertThat(adapter.loadFriendlyMatch(Long.MAX_VALUE)).isEmpty();
    }

    // 호스트를 제외한 팀원 입력 순서와 명단 확정 표시를 실제 DB에 저장한다.
    @Test
    void 호스트_명단의_입력_순서와_확정_표시를_저장하고_경기를_반환한다() {
        List<Long> members = List.of(secondMemberId, firstMemberId);

        FriendlyMatch savedMatch = adapter.saveHostRoster(expectedMatch.id(), members);
        flushAndClear();

        assertThat(storedMemberIds(expectedMatch.id())).containsExactlyElementsOf(members)
                .doesNotContain(host.id());
        assertThat(storedConfirmed(expectedMatch.id())).isTrue();
        assertThat(savedMatch).isEqualTo(expectedMatch);
        assertThat(adapter.loadFriendlyMatch(expectedMatch.id())).contains(expectedMatch);
    }

    // 비어 있지 않은 새 명단으로 교체하면 이전 팀원 행을 남기지 않는다.
    @Test
    void 기존_호스트_명단을_다른_명단으로_교체한다() {
        adapter.saveHostRoster(expectedMatch.id(), List.of(secondMemberId, firstMemberId));
        flushAndClear();
        List<Long> replacement = List.of(thirdMemberId);

        FriendlyMatch savedMatch = adapter.saveHostRoster(expectedMatch.id(), replacement);
        flushAndClear();

        assertThat(storedMemberIds(expectedMatch.id())).containsExactlyElementsOf(replacement);
        assertThat(storedConfirmed(expectedMatch.id())).isTrue();
        assertThat(savedMatch).isEqualTo(expectedMatch);
        assertThat(adapter.loadFriendlyMatch(expectedMatch.id())).contains(expectedMatch);
    }

    // 대상 경기 명단을 교체해도 다른 경기의 팀원과 확정 표시를 유지한다.
    @Test
    void 명단_교체는_다른_경기의_명단과_확정_표시에_영향을_주지_않는다() {
        FriendlyMatch anotherMatch = saveMatch("다른 경기입니다");
        List<Long> otherMembers = List.of(secondMemberId, thirdMemberId);
        adapter.saveHostRoster(expectedMatch.id(), List.of(firstMemberId));
        adapter.saveHostRoster(anotherMatch.id(), otherMembers);
        flushAndClear();

        adapter.saveHostRoster(expectedMatch.id(), List.of(thirdMemberId));
        flushAndClear();

        assertThat(storedMemberIds(expectedMatch.id())).containsExactly(thirdMemberId);
        assertThat(storedConfirmed(expectedMatch.id())).isTrue();
        assertThat(storedMemberIds(anotherMatch.id())).containsExactlyElementsOf(otherMembers);
        assertThat(storedConfirmed(anotherMatch.id())).isTrue();
        assertThat(adapter.loadFriendlyMatch(anotherMatch.id())).contains(anotherMatch);
    }

    // 실제 사용자와 연결한 게임 프로필을 저장하고 도메인 값으로 반환한다.
    private GameUserProfile saveProfile(String nickname) {
        UserJpaEntity user = userRepository.saveAndFlush(UserJpaEntity.create());
        GameUserJpaEntity profile = profileRepository.saveAndFlush(
                GameUserJpaEntity.of(user.getId(), game.id(), nickname)
        );
        return new GameUserProfile(profile.getId(), nickname, game, user.getId());
    }

    // 기존 경기 Entity로 저장하고 DB 식별자가 포함된 기대 도메인을 만든다.
    private FriendlyMatch saveMatch(String comment) {
        FriendlyMatch match = new FriendlyMatch(null, host, game, LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0), FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING,
                5, comment);
        Long id = matchRepository.saveAndFlush(FriendlyMatchJpaEntity.from(match)).getId();
        return new FriendlyMatch(id, host, game, match.date(), match.time(), match.sort(),
                match.state(), match.capacity(), match.comment());
    }

    // 팀원 테이블을 입력 순서대로 직접 읽어 실제 저장 행을 확인한다.
    private List<Long> storedMemberIds(Long matchId) {
        return jdbcTemplate.queryForList(
                "select game_user_id from friendly_match_host_member where match_id = ? order by member_order",
                Long.class, matchId
        );
    }

    // 경기 행의 내부 확정 표시를 직접 읽는다.
    private Boolean storedConfirmed(Long matchId) {
        return jdbcTemplate.queryForObject(
                "select host_roster_confirmed from friendly_match where id = ?", Boolean.class, matchId
        );
    }

    // 지연된 SQL을 실행하고 영속성 문맥의 캐시를 제거한다.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

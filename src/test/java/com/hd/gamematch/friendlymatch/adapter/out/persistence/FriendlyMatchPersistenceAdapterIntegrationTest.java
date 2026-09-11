package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(FriendlyMatchPersistenceAdapter.class)
class FriendlyMatchPersistenceAdapterIntegrationTest {

    @Autowired
    private FriendlyMatchPersistenceAdapter friendlyMatchPersistenceAdapter;

    @Autowired
    private FriendlyMatchJpaRepository friendlyMatchJpaRepository;

    // ID 없는 친선 경기 모집을 저장하면 DB ID가 담긴 생성 결과를 반환한다.
    @Test
    void 친선_경기_모집을_저장한다() {
        // 저장 전에는 DB 식별자가 없는 호스트 프로필과 친선 경기 모집을 준비한다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        FriendlyMatch friendlyMatch = new FriendlyMatch(
                null,
                host,
                game,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                FriendlyMatchState.RECRUITING,
                5,
                "저녁에 같이 하실 분"
        );

        // Adapter를 통해 실제 테스트 DB에 친선 경기 모집을 저장한다.
        FriendlyMatch savedFriendlyMatch = friendlyMatchPersistenceAdapter.save(friendlyMatch);

        // 저장 결과에는 DB가 발급한 ID가 담겨야 한다.
        assertThat(savedFriendlyMatch.id()).isNotNull();

        // Repository 재조회로 Adapter가 도메인 값을 올바른 컬럼에 저장했는지 검증한다.
        FriendlyMatchJpaEntity savedEntity = friendlyMatchJpaRepository
                .findById(savedFriendlyMatch.id())
                .orElseThrow();

        assertThat(savedEntity.getHostGameUserId()).isEqualTo(10L);
        assertThat(savedEntity.getGameId()).isEqualTo(1L);
        assertThat(savedEntity.getDate()).isEqualTo(LocalDate.of(2027, 6, 10));
        assertThat(savedEntity.getTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(savedEntity.getSort()).isEqualTo(FriendlyMatchSort.TEAM);
        assertThat(savedEntity.getState()).isEqualTo(FriendlyMatchState.RECRUITING);
        assertThat(savedEntity.getRecruit()).isEqualTo(5);
        assertThat(savedEntity.getComment()).isEqualTo("저녁에 같이 하실 분");
    }
}

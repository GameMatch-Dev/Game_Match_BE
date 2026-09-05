package com.hd.gamematch.gameuser.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.domain.GameUser;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(GameUserPersistenceAdapter.class)
class GameUserPersistenceAdapterIntegrationTest {

    @Autowired
    private GameUserPersistenceAdapter gameUserPersistenceAdapter;

    @Autowired
    private GameUserJpaRepository gameUserJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private GameJpaRepository gameJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void 게임_프로필을_저장한다() {
        // Given
        GameUser gameUser = GameUser.create(1L, 10L, "playerA");

        // When
        Long gameUserId = gameUserPersistenceAdapter.save(gameUser);

        // Then
        GameUserJpaEntity savedGameUser =
                gameUserJpaRepository.findById(gameUserId).orElseThrow();

        assertThat(gameUserId).isNotNull();
        assertThat(savedGameUser.getUserId()).isEqualTo(1L);
        assertThat(savedGameUser.getGameId()).isEqualTo(10L);
        assertThat(savedGameUser.getNickname()).isEqualTo("playerA");
    }

    @Test
    void 같은_사용자와_게임의_프로필이_있으면_참을_반환한다() {
        // Given: 이미 같은 사용자와 게임으로 저장된 프로필이 있다.
        gameUserPersistenceAdapter.save(
                GameUser.create(1L, 10L, "playerA")
        );

        // When: 같은 사용자·게임 조합의 프로필 존재 여부를 확인한다.
        boolean exists = gameUserPersistenceAdapter
                .existsByUserIdAndGameId(1L, 10L);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void 같은_사용자와_게임의_중복_프로필을_데이터베이스가_거부한다() {
        // Given: 이미 한 사용자가 특정 게임에 등록돼 있다.
        gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(1L, 10L, "playerA")
        );

        GameUserJpaEntity duplicateGameUser =
                GameUserJpaEntity.of(1L, 10L, "playerB");

        // When & Then: 닉네임이 달라도 같은 사용자·게임 조합은 DB가 거부한다.
        assertThatThrownBy(() ->
                gameUserJpaRepository.saveAndFlush(duplicateGameUser)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }


    @Test
    void 사용자와_게임_중복_제약_위반을_이미_등록됨_예외로_변환한다() {
        // Given: 이미 같은 사용자와 게임으로 저장된 프로필이 있다.
        gameUserPersistenceAdapter.save(
                GameUser.create(1L, 10L, "playerA")
        );

        // When & Then: DB의 유니크 제약 위반은 GameUser 업무 예외로 바뀐다.
        assertThatThrownBy(() ->
                gameUserPersistenceAdapter.save(
                        GameUser.create(1L, 10L, "playerB")
                )
        ).isInstanceOf(GameUserAlreadyRegisteredException.class);
    }

    @Test
    void 게임과_닉네임_중복_제약_위반을_닉네임_사용_중_예외로_변환한다() {
        // Given: 첫 사용자가 게임 10에서 playerA 닉네임을 이미 사용 중이다.
        gameUserPersistenceAdapter.save(
                GameUser.create(1L, 10L, "playerA")
        );

        // When & Then: 다른 사용자가 같은 게임에서 같은 닉네임을 저장하면
        // DB 제약 위반을 닉네임 중복 업무 예외로 변환한다.
        assertThatThrownBy(() ->
                gameUserPersistenceAdapter.save(
                        GameUser.create(2L, 10L, "playerA")
                )
        ).isInstanceOf(GameUserNicknameAlreadyInUseException.class);
    }

    @Test
    void 다른_무결성_위반은_이미_등록됨_예외로_변환하지_않는다() {
        // Given: nickname은 DB에서 NULL을 허용하지 않는 별도의 무결성 규칙이다.
        GameUser gameUserWithMissingNickname = GameUser.create(1L, 10L, null);

        // When & Then: 사용자·게임 중복과 관계없는 오류는 원래 예외로 남긴다.
        assertThatThrownBy(() ->
                gameUserPersistenceAdapter.save(gameUserWithMissingNickname)
        )
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(GameUserAlreadyRegisteredException.class);
    }

    @Test
    void 같은_게임에서_닉네임이_있으면_참을_반환한다() {
        // Given: 특정 게임에서 playerA 닉네임을 사용하는 프로필이 이미 있다.
        gameUserPersistenceAdapter.save(
                GameUser.create(1L, 10L, "playerA")
        );

        // When: 같은 게임과 닉네임 조합의 존재 여부를 확인한다.
        boolean exists = gameUserPersistenceAdapter
                .existsByGameIdAndNickname(10L, "playerA");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void 같은_게임의_중복_닉네임을_데이터베이스가_거부한다() {
        // Given: 첫 사용자가 게임 10에서 playerA 닉네임을 사용 중이다.
        gameUserJpaRepository.saveAndFlush(
        
                GameUserJpaEntity.of(1L, 10L, "playerA")
        );

        // 사용자 ID는 다르지만, 게임과 닉네임은 같다.
        GameUserJpaEntity duplicateNicknameGameUser =
                GameUserJpaEntity.of(2L, 10L, "playerA");

        // When & Then: 같은 게임에서는 같은 닉네임을 DB가 거부해야 한다.
        assertThatThrownBy(() ->
                gameUserJpaRepository.saveAndFlush(duplicateNicknameGameUser)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 사용자와_게임으로_게임_프로필을_조회한다() {
        // Given: 연결된 사용자, 게임, 게임 프로필을 모두 저장한다.
        UserJpaEntity user = userJpaRepository.save(UserJpaEntity.create());
        GameJpaEntity game = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameUserJpaEntity gameUser = gameUserJpaRepository.save(
                GameUserJpaEntity.of(user.getId(), game.getId(), "playerA")
        );

        // When
        var result = gameUserPersistenceAdapter.loadGameUserByUserIdAndGameId(
                user.getId(),
                game.getId()
        );

        // Then
        assertThat(result).isPresent();
        GameUserProfile profile = result.orElseThrow();
        assertThat(profile.id()).isEqualTo(gameUser.getId());
        assertThat(profile.nickname()).isEqualTo("playerA");
        assertThat(profile.userId()).isEqualTo(user.getId());
        assertThat(profile.game().id()).isEqualTo(game.getId());
        assertThat(profile.game().name()).isEqualTo("League of Legends");
    }

    @Test
    void 사용자와_게임에_해당하는_프로필이_없으면_빈_값을_반환한다() {
        var result = gameUserPersistenceAdapter.loadGameUserByUserIdAndGameId(999L, 10L);

        assertThat(result).isEmpty();
    }

    @Test
    void 닉네임_접두사와_게임_조건으로_프로필_목록과_전체_개수를_조회한다() {
        // given
        GameJpaEntity leagueOfLegends = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameJpaEntity valorant = gameJpaRepository.save(
                GameJpaEntity.of("Valorant", "FPS", "https://example.com/valorant")
        );
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity thirdUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity fourthUser = userJpaRepository.save(UserJpaEntity.create());

        gameUserJpaRepository.saveAll(List.of(
                GameUserJpaEntity.of(firstUser.getId(), leagueOfLegends.getId(), "playerA"),
                GameUserJpaEntity.of(secondUser.getId(), leagueOfLegends.getId(), "PlayerB"),
                GameUserJpaEntity.of(thirdUser.getId(), valorant.getId(), "playerC"),
                GameUserJpaEntity.of(fourthUser.getId(), leagueOfLegends.getId(), "proplayer")
        ));

        // when
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("PLAYER", leagueOfLegends.getId(), 1, 10);
        long totalCount = gameUserPersistenceAdapter
                .countGameUsersByNicknamePrefix("PLAYER", leagueOfLegends.getId());

        // then
        assertThat(profiles)
                .extracting(GameUserProfile::nickname)
                .containsExactly("playerA", "PlayerB");
        assertThat(totalCount).isEqualTo(2L);
    }

    @Test
    void LIKE_특수문자가_포함된_닉네임_접두사는_문자_그대로_검색한다() {
        // given
        GameJpaEntity game = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        UserJpaEntity literalPrefixUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity wildcardCandidateUser = userJpaRepository.save(UserJpaEntity.create());

        gameUserJpaRepository.saveAll(List.of(
                GameUserJpaEntity.of(literalPrefixUser.getId(), game.getId(), "player_1"),
                GameUserJpaEntity.of(wildcardCandidateUser.getId(), game.getId(), "playerA1")
        ));

        // when
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("player_", game.getId(), 1, 10);
        long totalCount = gameUserPersistenceAdapter
                .countGameUsersByNicknamePrefix("player_", game.getId());

        // then: '_'는 임의 한 글자가 아니라 닉네임의 실제 문자다.
        assertThat(profiles)
                .extracting(GameUserProfile::nickname)
                .containsExactly("player_1");
        assertThat(totalCount).isEqualTo(1L);
    }

    @Test
    void 퍼센트가_포함된_닉네임_접두사는_문자_그대로_검색한다() {
        // given
        GameJpaEntity game = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        UserJpaEntity literalPrefixUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity wildcardCandidateUser = userJpaRepository.save(UserJpaEntity.create());

        gameUserJpaRepository.saveAll(List.of(
                GameUserJpaEntity.of(literalPrefixUser.getId(), game.getId(), "player%1"),
                GameUserJpaEntity.of(wildcardCandidateUser.getId(), game.getId(), "playerA1")
        ));

        // when
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("player%", game.getId(), 1, 10);
        long totalCount = gameUserPersistenceAdapter
                .countGameUsersByNicknamePrefix("player%", game.getId());

        // then: '%'는 임의 길이 문자열이 아니라 닉네임의 실제 문자다.
        assertThat(profiles)
                .extracting(GameUserProfile::nickname)
                .containsExactly("player%1");
        assertThat(totalCount).isEqualTo(1L);
    }

    @Test
    void 백슬래시가_포함된_닉네임_접두사는_문자_그대로_검색한다() {
        // given: Java 문자열의 \\는 실제 닉네임의 백슬래시 한 글자를 뜻한다.
        GameJpaEntity game = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        UserJpaEntity literalPrefixUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity nonMatchingUser = userJpaRepository.save(UserJpaEntity.create());

        gameUserJpaRepository.saveAll(List.of(
                GameUserJpaEntity.of(literalPrefixUser.getId(), game.getId(), "player\\one"),
                GameUserJpaEntity.of(nonMatchingUser.getId(), game.getId(), "playerXone")
        ));

        // when
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("player\\", game.getId(), 1, 10);
        long totalCount = gameUserPersistenceAdapter
                .countGameUsersByNicknamePrefix("player\\", game.getId());

        // then: '\\'는 LIKE 이스케이프 문자로 소비되지 않고 닉네임의 실제 문자다.
        assertThat(profiles)
                .extracting(GameUserProfile::nickname)
                .containsExactly("player\\one");
        assertThat(totalCount).isEqualTo(1L);
    }

    @Test
    void 검색_목록을_조회할_때_프로필마다_추가_쿼리를_실행하지_않는다() {
        // given
        // 서로 다른 Game/User를 연결해야 각 프로필이 별도의 연관 데이터를 요구하는 상황을 재현한다.
        GameJpaEntity firstGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameJpaEntity secondGame = gameJpaRepository.save(
                GameJpaEntity.of("Valorant", "FPS", "https://example.com/valorant")
        );
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());
        gameUserJpaRepository.saveAll(List.of(
                GameUserJpaEntity.of(firstUser.getId(), firstGame.getId(), "playerA"),
                GameUserJpaEntity.of(secondUser.getId(), secondGame.getId(), "playerB")
        ));

        // fixture 저장 INSERT를 먼저 끝내고, 1차 캐시를 비워 검색 시 실제 SELECT가 실행되게 한다.
        entityManager.flush();
        entityManager.clear();

        // Hibernate Statistics에는 setup 쿼리도 기록되므로, 검색 직전에 카운터를 초기화한다.
        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // when
        // HTTP 계층이 아니라 N+1이 발생하는 Persistence Adapter의 목록 조회만 직접 측정한다.
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("player", null, 1, 10);

        // 결과가 실제로 2건이어야 쿼리가 적게 실행돼도 통과하는 거짓 양성을 막을 수 있다.
        assertThat(profiles).hasSize(2);

        // 목록 1회 + Game 일괄 조회 1회 + User 일괄 조회 1회를 넘으면 N+1 회귀로 본다.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3L);
    }

    @Test
    void 검색_목록의_프로필에_연결된_게임이_없으면_정합성_오류를_던진다() {
        // given: game_user에는 행이 있지만 game_id가 가리키는 Game 행은 없다.
        UserJpaEntity user = userJpaRepository.save(UserJpaEntity.create());
        gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(user.getId(), 999_999L, "playerA")
        );

        // when & then
        assertThatThrownBy(() -> gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("player", null, 1, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("게임 프로필의 연결 게임이 존재하지 않습니다.");
    }

    @Test
    void 검색_목록의_프로필에_연결된_사용자가_없으면_정합성_오류를_던진다() {
        // given: game_user에는 행이 있지만 user_id가 가리키는 User 행은 없다.
        GameJpaEntity game = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(999_999L, game.getId(), "playerA")
        );

        // when & then
        assertThatThrownBy(() -> gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("player", null, 1, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("게임 프로필의 연결 사용자가 존재하지 않습니다.");
    }

    @Test
    void 게임_조건이_없으면_서로_다른_게임의_닉네임_접두사_결과를_모두_조회한다() {
        // given
        GameJpaEntity leagueOfLegends = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameJpaEntity valorant = gameJpaRepository.save(
                GameJpaEntity.of("Valorant", "FPS", "https://example.com/valorant")
        );
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());

        gameUserJpaRepository.saveAll(List.of(
                GameUserJpaEntity.of(firstUser.getId(), leagueOfLegends.getId(), "playerA"),
                GameUserJpaEntity.of(secondUser.getId(), valorant.getId(), "PlayerB")
        ));

        // when
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("PLAYER", null, 1, 10);
        long totalCount = gameUserPersistenceAdapter
                .countGameUsersByNicknamePrefix("PLAYER", null);

        // then
        assertThat(profiles)
                .extracting(GameUserProfile::nickname)
                .containsExactly("playerA", "PlayerB");
        assertThat(profiles)
                .extracting(profile -> profile.game().id())
                .containsExactly(leagueOfLegends.getId(), valorant.getId());
        assertThat(totalCount).isEqualTo(2L);
    }

    @Test
    void 일치하는_닉네임_접두사가_없으면_빈_목록과_0개를_반환한다() {
        // when
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("unknown", null, 1, 10);
        long totalCount = gameUserPersistenceAdapter
                .countGameUsersByNicknamePrefix("unknown", null);

        // then
        assertThat(profiles).isEmpty();
        assertThat(totalCount).isZero();
    }

    @Test
    void 닉네임과_ID_오름차순으로_정렬한_두번째_페이지를_조회한다() {
        // given: 대소문자만 다른 닉네임은 ID로, 그 외에는 닉네임으로 순서를 결정한다.
        GameJpaEntity firstGame = gameJpaRepository.save(
                GameJpaEntity.of("League of Legends", "MOBA", "https://example.com/lol")
        );
        GameJpaEntity secondGame = gameJpaRepository.save(
                GameJpaEntity.of("Valorant", "FPS", "https://example.com/valorant")
        );
        GameJpaEntity thirdGame = gameJpaRepository.save(
                GameJpaEntity.of("Overwatch", "FPS", "https://example.com/overwatch")
        );
        UserJpaEntity firstUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity secondUser = userJpaRepository.save(UserJpaEntity.create());
        UserJpaEntity thirdUser = userJpaRepository.save(UserJpaEntity.create());

        GameUserJpaEntity firstProfile = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(firstUser.getId(), firstGame.getId(), "player")
        );
        GameUserJpaEntity secondProfile = gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(secondUser.getId(), secondGame.getId(), "Player")
        );
        gameUserJpaRepository.saveAndFlush(
                GameUserJpaEntity.of(thirdUser.getId(), thirdGame.getId(), "playerZ")
        );

        // when
        List<GameUserProfile> profiles = gameUserPersistenceAdapter
                .loadGameUsersByNicknamePrefix("PLAYER", null, 2, 1);
        long totalCount = gameUserPersistenceAdapter
                .countGameUsersByNicknamePrefix("PLAYER", null);

        // then: 첫 두 행은 같은 대소문자 무시 닉네임이므로 ID가 작은 행이 먼저다.
        assertThat(firstProfile.getId()).isLessThan(secondProfile.getId());
        assertThat(profiles)
                .extracting(GameUserProfile::id)
                .containsExactly(secondProfile.getId());
        assertThat(totalCount).isEqualTo(3L);
    }
}

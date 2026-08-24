package com.hd.gamematch.gameuser.adapter.out.persistence;

import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.domain.GameUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(GameUserPersistenceAdapter.class)
class GameUserPersistenceAdapterIntegrationTest {

    @Autowired
    private GameUserPersistenceAdapter gameUserPersistenceAdapter;

    @Autowired
    private GameUserJpaRepository gameUserJpaRepository;

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
}

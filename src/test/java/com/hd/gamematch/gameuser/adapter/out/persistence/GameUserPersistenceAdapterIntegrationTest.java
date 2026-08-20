package com.hd.gamematch.gameuser.adapter.out.persistence;

import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
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
    void saveStoresGameUser() {
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
    void existsReturnsTrueWhenSameUserAndGameProfileExists() {
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
    void databaseRejectsDuplicateGameUserForSameUserAndGame() {
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
    void saveConvertsDuplicateConstraintViolationToGameUserAlreadyRegisteredException() {
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
    void saveDoesNotConvertOtherIntegrityViolationsToDuplicateRegistrationException() {
        // Given: nickname은 DB에서 NULL을 허용하지 않는 별도의 무결성 규칙이다.
        GameUser gameUserWithMissingNickname = GameUser.create(1L, 10L, null);

        // When & Then: 사용자·게임 중복과 관계없는 오류는 원래 예외로 남긴다.
        assertThatThrownBy(() ->
                gameUserPersistenceAdapter.save(gameUserWithMissingNickname)
        )
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(GameUserAlreadyRegisteredException.class);
    }
}

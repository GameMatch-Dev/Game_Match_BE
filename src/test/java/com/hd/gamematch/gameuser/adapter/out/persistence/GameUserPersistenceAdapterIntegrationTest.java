package com.hd.gamematch.gameuser.adapter.out.persistence;

import com.hd.gamematch.gameuser.domain.GameUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

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
}

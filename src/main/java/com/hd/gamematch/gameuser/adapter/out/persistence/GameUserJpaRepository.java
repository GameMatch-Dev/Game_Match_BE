package com.hd.gamematch.gameuser.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameUserJpaRepository
        extends JpaRepository<GameUserJpaEntity, Long> {

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    boolean existsByGameIdAndNickname(Long gameId, String nickname);

    Optional<GameUserJpaEntity> findByUserIdAndGameId(Long userId, Long gameId);
}
package com.hd.gamematch.gameuser.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameUserJpaRepository
        extends JpaRepository<GameUserJpaEntity, Long> {

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    boolean existsByGameIdAndNickname(Long gameId, String nickname);

    Optional<GameUserJpaEntity> findByUserIdAndGameId(Long userId, Long gameId);

    @Query("""
            select gameUser
            from GameUserJpaEntity gameUser
            where lower(gameUser.nickname) like lower(concat(:nickname, '%'))
              and (:gameId is null or gameUser.gameId = :gameId)
            order by lower(gameUser.nickname) asc, gameUser.id asc
            """)
    List<GameUserJpaEntity> findByNicknamePrefix(
            @Param("nickname") String nickname,
            @Param("gameId") Long gameId,
            Pageable pageable
    );

    @Query("""
            select count(gameUser)
            from GameUserJpaEntity gameUser
            where lower(gameUser.nickname) like lower(concat(:nickname, '%'))
              and (:gameId is null or gameUser.gameId = :gameId)
            """)
    long countByNicknamePrefix(
            @Param("nickname") String nickname,
            @Param("gameId") Long gameId
    );
}

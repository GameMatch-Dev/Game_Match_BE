package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

// FriendlyMatchJpaEntity의 기본 저장과 단건 조회를 Spring Data JPA에 위임한다.
public interface FriendlyMatchJpaRepository extends JpaRepository<FriendlyMatchJpaEntity, Long> {

    // 승인에 영향을 주는 변경을 같은 경기 행의 쓰기 잠금으로 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select match from FriendlyMatchJpaEntity match where match.id = :id")
    Optional<FriendlyMatchJpaEntity> findByIdForUpdate(@Param("id") Long id);
}

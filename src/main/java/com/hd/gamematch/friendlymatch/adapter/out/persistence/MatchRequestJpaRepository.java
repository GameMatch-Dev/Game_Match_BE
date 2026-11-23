package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchRequestJpaRepository extends JpaRepository<MatchRequestJpaEntity, Long> {

    // 경기와 신청자 조합에 해당하는 신청을 조회한다.
    Optional<MatchRequestJpaEntity> findByMatchIdAndGameUserId(Long matchId, Long gameUserId);
}

package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface MatchParticipantJpaRepository extends JpaRepository<MatchParticipantJpaEntity, Long> {

    // 경기 잠금 이후의 현재 참가자를 신청과 명단 순서대로 읽는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MatchParticipantJpaEntity> findByMatchIdOrderByRequestIdAscMemberOrderAsc(Long matchId);
}

package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface MatchRequestJpaRepository extends JpaRepository<MatchRequestJpaEntity, Long> {

    // 최초 조회는 식별자만 읽어 신청 Entity의 오래된 상태를 캐시하지 않는다.
    @Query("select request.matchId from MatchRequestJpaEntity request where request.id = :id")
    Optional<Long> findMatchIdByRequestId(@Param("id") Long id);

    // 경기 잠금을 얻은 후 신청 행의 최신 상태를 잠금 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from MatchRequestJpaEntity request where request.id = :id")
    Optional<MatchRequestJpaEntity> findByIdForUpdate(@Param("id") Long id);

    // 일반 스냅샷 조회 대신 잠금 읽기로 현재 승인된 신청을 확인한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MatchRequestJpaEntity> findByMatchIdAndStateOrderById(Long matchId, MatchRequestState state);

    // 경기와 신청자 조합에 해당하는 신청을 조회한다.
    Optional<MatchRequestJpaEntity> findByMatchIdAndGameUserId(Long matchId, Long gameUserId);

    // 비활성 상태인 행만 원자적으로 재활성화해 동시 재신청의 승자를 정한다.
    @Modifying(flushAutomatically = true)
    @Query("""
            update MatchRequestJpaEntity request
               set request.state = :pendingState, request.comment = :comment
             where request.id = :id
               and request.matchId = :matchId
               and request.gameUserId = :gameUserId
               and request.state in :inactiveStates
            """)
    int reactivateIfInactive(@Param("id") Long id,
                             @Param("matchId") Long matchId,
                             @Param("gameUserId") Long gameUserId,
                             @Param("pendingState") MatchRequestState pendingState,
                             @Param("comment") String comment,
                             @Param("inactiveStates") Collection<MatchRequestState> inactiveStates);
}

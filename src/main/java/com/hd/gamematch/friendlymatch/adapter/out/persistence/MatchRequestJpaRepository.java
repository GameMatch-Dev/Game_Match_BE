package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface MatchRequestJpaRepository extends JpaRepository<MatchRequestJpaEntity, Long> {

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

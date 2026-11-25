package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 승인된 신청의 실제 참가자만 저장하며 주최 명단과 구분한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friendly_match_participant", uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_participant_profile", columnNames = {"match_id", "game_user_id"}),
        @UniqueConstraint(name = "uk_match_participant_order", columnNames = {"request_id", "member_order"})
})
public class MatchParticipantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;
    @Column(name = "request_id", nullable = false)
    private Long requestId;
    @Column(name = "game_user_id", nullable = false)
    private Long gameUserId;
    @Column(name = "member_order", nullable = false)
    private int memberOrder;

    // 신청 대표부터 팀원까지 전달받은 참가 순서를 보존한다.
    public static MatchParticipantJpaEntity of(Long matchId, Long requestId, Long gameUserId, int memberOrder) {
        MatchParticipantJpaEntity entity = new MatchParticipantJpaEntity();
        entity.matchId = matchId;
        entity.requestId = requestId;
        entity.gameUserId = gameUserId;
        entity.memberOrder = memberOrder;
        return entity;
    }
}

package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "match_request", uniqueConstraints = @UniqueConstraint(
        name = "uk_match_request_match_id_game_user_id", columnNames = {"match_id", "game_user_id"}))
public class MatchRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "game_user_id", nullable = false)
    private Long gameUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchRequestState state;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ElementCollection
    @CollectionTable(name = "match_request_member", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "game_user_id", nullable = false)
    @OrderColumn(name = "member_order")
    private List<Long> teamMemberIds = new ArrayList<>();

    // 새 신청의 저장 정보를 가진 Entity를 만든다.
    public static MatchRequestJpaEntity from(MatchRequest request) {
        MatchRequestJpaEntity entity = new MatchRequestJpaEntity();
        entity.update(request);
        return entity;
    }

    // 기존 신청 행과 팀원 목록을 새 신청 내용으로 갱신한다.
    public void update(MatchRequest request) {
        matchId = request.matchId();
        gameUserId = request.gameUserId();
        state = request.state();
        comment = request.comment();

        // 관리 중인 컬렉션을 유지하며 이전 팀원과 순서를 교체한다.
        teamMemberIds.clear();
        teamMemberIds.addAll(request.teamMemberIds());
    }

    // 저장된 상태와 팀원 순서를 도메인 값으로 복원한다.
    public MatchRequest toDomain() {
        return new MatchRequest(id, matchId, gameUserId, List.copyOf(teamMemberIds), comment, state);
    }
}

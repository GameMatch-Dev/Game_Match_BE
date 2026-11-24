package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friendly_match")
public class FriendlyMatchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_game_user_id", nullable = false)
    private Long hostGameUserId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "match_date", nullable = false)
    private LocalDate date;

    @Column(name = "match_time", nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendlyMatchSort sort;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendlyMatchState state;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, length = 500)
    private String comment;

    @Column(name = "host_roster_confirmed", nullable = false)
    private boolean hostRosterConfirmed;

    @ElementCollection
    @CollectionTable(name = "friendly_match_host_member", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "game_user_id", nullable = false)
    @OrderColumn(name = "member_order")
    private List<Long> hostTeamMemberIds = new ArrayList<>();

    private FriendlyMatchJpaEntity(
            Long hostGameUserId,
            Long gameId,
            LocalDate date,
            LocalTime time,
            FriendlyMatchSort sort,
            FriendlyMatchState state,
            Integer capacity,
            String comment
    ) {
        this.hostGameUserId = hostGameUserId;
        this.gameId = gameId;
        this.date = date;
        this.time = time;
        this.sort = sort;
        this.state = state;
        this.capacity = capacity;
        this.comment = comment;
    }

    // 관리 중인 컬렉션을 유지하며 호스트 팀원과 순서를 교체하고 확정 표시를 저장한다.
    public void confirmHostRoster(List<Long> teamMemberIds) {
        hostTeamMemberIds.clear();
        hostTeamMemberIds.addAll(teamMemberIds);
        hostRosterConfirmed = true;
    }

    // FriendlyMatch 도메인 객체를 friendly_match 테이블 행으로 변환한다.
    public static FriendlyMatchJpaEntity from(FriendlyMatch friendlyMatch) {
        return new FriendlyMatchJpaEntity(
                friendlyMatch.host().id(),
                friendlyMatch.game().id(),
                friendlyMatch.date(),
                friendlyMatch.time(),
                friendlyMatch.sort(),
                friendlyMatch.state(),
                friendlyMatch.capacity(),
                friendlyMatch.comment()
        );
    }
}

package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

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
    private Integer recruit;

    @Column(nullable = false, length = 500)
    private String comment;

    private FriendlyMatchJpaEntity(
            Long hostGameUserId,
            Long gameId,
            LocalDate date,
            LocalTime time,
            FriendlyMatchSort sort,
            FriendlyMatchState state,
            Integer recruit,
            String comment
    ) {
        this.hostGameUserId = hostGameUserId;
        this.gameId = gameId;
        this.date = date;
        this.time = time;
        this.sort = sort;
        this.state = state;
        this.recruit = recruit;
        this.comment = comment;
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
                friendlyMatch.recruit(),
                friendlyMatch.comment()
        );
    }
}

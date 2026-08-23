package com.hd.gamematch.gameuser.adapter.out.persistence;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "game_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_user_user_id_game_id",
                        columnNames = {"user_id", "game_id"}
                ),
                @UniqueConstraint(
                        name = "uk_game_user_game_id_nickname",
                        columnNames = {"game_id", "nickname"}
                )
        }
)
public class GameUserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private String nickname;

    private GameUserJpaEntity(
            Long userId,
            Long gameId,
            String nickname
    ) {
        this.userId = userId;
        this.gameId = gameId;
        this.nickname = nickname;
    }

    public static GameUserJpaEntity of(
            Long userId,
            Long gameId,
            String nickname
    ) {
        return new GameUserJpaEntity(userId, gameId, nickname);
    }
}

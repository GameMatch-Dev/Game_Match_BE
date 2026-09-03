package com.hd.gamematch.game.application.port.in.find;

public record FindGameQuery (
        Long gameId
)
{
    public FindGameQuery {
        if(gameId == null){
            throw new IllegalArgumentException("gameId는 필수입니다.");
        }

        if(gameId <= 0){
            throw new IllegalArgumentException("gameId는 1 이상이어야 합니다.");
        }
    }

    public static FindGameQuery of(Long gameId){
        return new FindGameQuery(gameId);
    }
}

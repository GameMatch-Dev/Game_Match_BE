package com.hd.gamematch.game.adapter.in.web.response;

import com.hd.gamematch.game.domain.Game;

public record GameResponse(
        Long id,
        String name,
        String sort,
        String url
){
    public static GameResponse from(Game game){
        return new GameResponse(
                game.id(),
                game.name(),
                game.sort(),
                game.url()
        );
    }
}

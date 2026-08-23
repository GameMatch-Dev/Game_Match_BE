package com.hd.gamematch.gameuser.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GameUser {

    // 속성 작성
    private final Long userId;
    private final Long gameId;
    private final String nickname;


    // 이미 없던거를 만드는 것이기 때문에, GameUser.create로 쓸 수 있게하기 위해서 static으로 선언하였다.
    public static GameUser create(Long userId, Long gameId, String nickname){
        return new GameUser(userId, gameId, nickname);
    }
}

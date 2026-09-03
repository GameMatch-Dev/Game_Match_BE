package com.hd.gamematch.gameuser.adapter.in.web.request;

/**
 * POST /game-users 요청 JSON을 받는 Request DTO다.
 *
 * <p>userId는 요청 본문으로 받지 않는다. 인증된 JWT에서 서버가 직접 꺼낸다.</p>
 */
public record RegisterGameUserRequest(
        Long gameId,
        String nickname
) {
}

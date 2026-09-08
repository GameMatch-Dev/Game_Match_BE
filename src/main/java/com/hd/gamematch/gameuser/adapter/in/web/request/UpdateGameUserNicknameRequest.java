package com.hd.gamematch.gameuser.adapter.in.web.request;

/**
 * 게임 프로필 닉네임 수정 요청의 HTTP 본문이다.
 */
public record UpdateGameUserNicknameRequest(
        String nickname
) {
}

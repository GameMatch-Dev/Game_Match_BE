package com.hd.gamematch.gameuser.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 게임 프로필 닉네임 수정 요청의 HTTP 본문이다.
 */
public record UpdateGameUserNicknameRequest(
        @NotBlank(message = "nickname은 비어 있을 수 없습니다.") String nickname
) {
}

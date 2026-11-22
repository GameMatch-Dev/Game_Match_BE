package com.hd.gamematch.gameuser.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 게임 프로필 닉네임 수정 요청의 HTTP 본문이다.
 */
public record UpdateGameUserNicknameRequest(
        @NotBlank(message = "nickname은 비어 있을 수 없습니다.") String nickname
) {
    public UpdateGameUserNicknameRequest {
        // 요청 경계에서 한 번 정규화해 검증, 동일 값 판단, 중복 조회와 저장이 같은 값을 사용한다.
        if (nickname != null) {
            nickname = nickname.trim();
        }
    }
}

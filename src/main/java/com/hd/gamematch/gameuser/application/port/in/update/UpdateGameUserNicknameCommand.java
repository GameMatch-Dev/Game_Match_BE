package com.hd.gamematch.gameuser.application.port.in.update;

/**
 * 게임 프로필 닉네임 수정에 필요한 입력값을 묶는다.
 */
public record UpdateGameUserNicknameCommand(
        Long userId,
        Long gameUserId,
        String nickname
) {
    // 정적 팩토리 메서드
    public static UpdateGameUserNicknameCommand of(
            Long userId,
            Long gameUserId,
            String nickname
    ) {
        return new UpdateGameUserNicknameCommand(userId, gameUserId, nickname);
    }
}

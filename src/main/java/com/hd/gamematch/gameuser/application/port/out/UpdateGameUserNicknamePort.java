package com.hd.gamematch.gameuser.application.port.out;

/**
 * 저장된 게임 프로필의 닉네임을 변경한다.
 */
public interface UpdateGameUserNicknamePort {

    void updateNickname(Long gameUserId, String nickname);
}

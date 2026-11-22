package com.hd.gamematch.gameuser.application.port.in.update;

import com.hd.gamematch.gameuser.domain.GameUserProfile;

/**
 * 인증된 사용자의 게임 프로필 닉네임을 수정한다.
 */
public interface UpdateGameUserNicknameUseCase {

    GameUserProfile updateNickname(UpdateGameUserNicknameCommand command);
}

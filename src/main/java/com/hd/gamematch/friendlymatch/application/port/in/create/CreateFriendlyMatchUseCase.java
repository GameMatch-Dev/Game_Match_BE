package com.hd.gamematch.friendlymatch.application.port.in.create;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;

public interface CreateFriendlyMatchUseCase {

    /**
     * 인증된 사용자의 게임 프로필을 호스트로 친선 경기 모집을 생성한다.
     */
    FriendlyMatch create(CreateFriendlyMatchCommand command);
}

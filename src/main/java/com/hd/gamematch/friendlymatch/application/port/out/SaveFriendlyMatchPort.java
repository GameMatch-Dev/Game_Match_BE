package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;

public interface SaveFriendlyMatchPort {

    // 새 친선 경기 모집을 저장하고, DB에서 발급된 ID가 담긴 결과를 반환한다.
    FriendlyMatch save(FriendlyMatch friendlyMatch);
}

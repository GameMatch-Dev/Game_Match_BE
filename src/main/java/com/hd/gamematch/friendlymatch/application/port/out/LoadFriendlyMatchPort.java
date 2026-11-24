package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;

import java.util.Optional;

public interface LoadFriendlyMatchPort {

    // 경기 ID로 조회하며 Optional.empty()는 조회 결과 없음을 뜻한다.
    Optional<FriendlyMatch> loadFriendlyMatch(Long matchId);
}

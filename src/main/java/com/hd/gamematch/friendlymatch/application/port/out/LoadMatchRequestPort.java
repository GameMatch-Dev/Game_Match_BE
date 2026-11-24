package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.MatchRequest;

import java.util.Optional;

// 경기와 신청 프로필에 해당하는 기존 신청을 조회한다.
public interface LoadMatchRequestPort {

    // 같은 경기와 게임 프로필의 기존 신청을 조회한다.
    Optional<MatchRequest> loadByMatchIdAndGameUserId(Long matchId, Long gameUserId);
}

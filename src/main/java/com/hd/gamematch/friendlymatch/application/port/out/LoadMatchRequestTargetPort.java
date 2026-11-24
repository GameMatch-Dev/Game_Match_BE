package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.MatchRequestTarget;

import java.util.Optional;

// 참가 신청에 필요한 대상 경기 정보를 조회한다.
public interface LoadMatchRequestTargetPort {

    // 경기 식별자로 신청 대상 정보를 조회한다.
    Optional<MatchRequestTarget> loadMatchRequestTarget(Long matchId);
}

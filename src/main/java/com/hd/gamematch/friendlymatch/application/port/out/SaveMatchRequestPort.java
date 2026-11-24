package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.MatchRequest;

// 참가 신청의 저장 경계를 정의한다.
public interface SaveMatchRequestPort {

    // 신청을 저장하고 저장된 신청 식별자를 반환한다.
    Long save(MatchRequest matchRequest);
}

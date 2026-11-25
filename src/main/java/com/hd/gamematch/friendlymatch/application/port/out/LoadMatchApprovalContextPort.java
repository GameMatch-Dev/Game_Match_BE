package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.MatchApprovalContext;

import java.util.Optional;

// 같은 경기의 승인과 명단 변경을 직렬화할 조회 경계를 정의한다.
public interface LoadMatchApprovalContextPort {

    // 경기 행을 비관적으로 잠그고 승인 판단에 필요한 현재 정보를 조회한다.
    Optional<MatchApprovalContext> loadForUpdate(Long matchId);
}

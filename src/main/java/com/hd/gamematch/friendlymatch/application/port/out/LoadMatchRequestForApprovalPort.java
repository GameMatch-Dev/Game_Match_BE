package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.MatchRequest;

import java.util.Optional;

// 승인 대상 경기 식별자와 잠금 이후의 최신 신청을 조회한다.
public interface LoadMatchRequestForApprovalPort {

    // 경기 행을 먼저 잠그기 위해 신청이 가리키는 경기 ID만 조회한다.
    Optional<Long> findMatchIdByRequestId(Long requestId);

    // 경기 잠금을 얻은 뒤 신청 상태와 명단을 다시 조회한다.
    Optional<MatchRequest> loadRequestById(Long requestId);
}

package com.hd.gamematch.friendlymatch.application.port.out;

import java.util.List;

// 승인으로 함께 바뀌어야 하는 신청과 참가자 및 경기 상태의 저장 경계다.
public interface SaveMatchRequestApprovalPort {

    // 호출자의 경기 잠금과 트랜잭션 안에서 참가자 등록과 승인 상태 및 모집 종료를 함께 저장한다.
    void saveApproval(Long matchId, Long requestId, List<Long> participantIds, boolean closeRecruitment);
}

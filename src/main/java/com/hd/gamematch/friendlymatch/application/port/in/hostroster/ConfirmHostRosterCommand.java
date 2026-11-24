package com.hd.gamematch.friendlymatch.application.port.in.hostroster;

import java.util.List;

// 호스트 명단 확정에 필요한 인증 사용자와 경기 및 팀원 ID를 묶는다.
public record ConfirmHostRosterCommand(
        Long userId,
        Long matchId,
        List<Long> teamMemberIds
) {

    // HTTP 요청에서 받은 명단과 인증 및 경로 정보를 Command로 전달한다.
    public static ConfirmHostRosterCommand of(Long userId, Long matchId, List<Long> teamMemberIds) {
        return new ConfirmHostRosterCommand(userId, matchId, teamMemberIds);
    }
}

package com.hd.gamematch.friendlymatch.application.port.in.request;

import java.util.List;

// 친선 경기 참가 신청에 필요한 요청값과 인증 주체를 하나로 묶는다.
public record CreateMatchRequestCommand(
        Long userId,
        Long matchId,
        Long gameUserId,
        List<Long> teamMemberIds,
        String comment
) {

    // Controller가 전달할 참가 신청 입력값을 Command로 묶는다.
    public static CreateMatchRequestCommand of(
            Long userId,
            Long matchId,
            Long gameUserId,
            List<Long> teamMemberIds,
            String comment
    ) {
        return new CreateMatchRequestCommand(
                userId,
                matchId,
                gameUserId,
                teamMemberIds,
                comment
        );
    }
}

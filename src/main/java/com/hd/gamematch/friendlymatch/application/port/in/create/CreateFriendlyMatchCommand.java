package com.hd.gamematch.friendlymatch.application.port.in.create;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 친선 경기 모집 생성에 필요한 요청값과 인증 주체를 하나로 묶는다.
 */
public record CreateFriendlyMatchCommand(
        Long userId,
        Long hostGameUserId,
        Long gameId,
        LocalDate date,
        LocalTime time,
        FriendlyMatchSort sort,
        Integer capacity,
        String comment,
        List<Long> hostTeamMemberIds
) {

    // Controller가 전달할 친선 경기 모집 생성 입력값을 Command로 묶는다.
    public static CreateFriendlyMatchCommand of(
            Long userId,
            Long hostGameUserId,
            Long gameId,
            LocalDate date,
            LocalTime time,
            FriendlyMatchSort sort,
            Integer capacity,
            String comment
    ) {
        // 기존 생성 호출은 호스트 명단을 미확정 상태로 전달한다.
        return of(userId, hostGameUserId, gameId, date, time, sort, capacity, comment, null);
    }

    // 선택한 호스트 명단을 생략 및 null 여부 그대로 생성 입력에 포함한다.
    public static CreateFriendlyMatchCommand of(
            Long userId,
            Long hostGameUserId,
            Long gameId,
            LocalDate date,
            LocalTime time,
            FriendlyMatchSort sort,
            Integer capacity,
            String comment,
            List<Long> hostTeamMemberIds
    ) {
        return new CreateFriendlyMatchCommand(
                userId,
                hostGameUserId,
                gameId,
                date,
                time,
                sort,
                capacity,
                comment,
                hostTeamMemberIds
        );
    }
}

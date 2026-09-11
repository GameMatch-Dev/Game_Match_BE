package com.hd.gamematch.friendlymatch.application.port.in.create;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;

import java.time.LocalDate;
import java.time.LocalTime;

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
        Integer recruit,
        String comment
) {

    // Controller가 전달할 친선 경기 모집 생성 입력값을 Command로 묶는다.
    public static CreateFriendlyMatchCommand of(
            Long userId,
            Long hostGameUserId,
            Long gameId,
            LocalDate date,
            LocalTime time,
            FriendlyMatchSort sort,
            Integer recruit,
            String comment
    ) {
        return new CreateFriendlyMatchCommand(
                userId,
                hostGameUserId,
                gameId,
                date,
                time,
                sort,
                recruit,
                comment
        );
    }
}

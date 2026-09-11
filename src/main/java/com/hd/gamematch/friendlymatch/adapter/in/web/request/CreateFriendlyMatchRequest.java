package com.hd.gamematch.friendlymatch.adapter.in.web.request;

import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchCommand;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateFriendlyMatchRequest(
        Long hostGameUserId,
        Long gameId,
        LocalDate date,
        LocalTime time,
        Integer sort,
        Integer recruit,
        String comment
) {

    // HTTP JSON 요청값과 JWT에서 얻은 사용자 ID를 친선 경기 생성 Command로 변환한다.
    public CreateFriendlyMatchCommand toCommand(Long userId) {
        return CreateFriendlyMatchCommand.of(
                userId,
                hostGameUserId,
                gameId,
                date,
                time,
                FriendlyMatchSort.values()[sort],
                recruit,
                comment
        );
    }
}

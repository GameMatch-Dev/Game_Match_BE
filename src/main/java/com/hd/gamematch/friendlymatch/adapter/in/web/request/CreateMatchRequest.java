package com.hd.gamematch.friendlymatch.adapter.in.web.request;

import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public record CreateMatchRequest(
        @JsonDeserialize(using = FriendlyMatchIntegerDeserializers.LongValue.class)
        @NotNull @Positive Long gameUserId,
        @JsonDeserialize(contentUsing = FriendlyMatchIntegerDeserializers.LongValue.class)
        List<Long> teamMemberIds,
        String comment
) {

    // 요청 본문과 인증 사용자 및 경기 ID를 참가 신청 Command로 변환한다.
    public CreateMatchRequestCommand toCommand(Long userId, Long matchId) {
        return CreateMatchRequestCommand.of(userId, matchId, gameUserId, teamMemberIds, comment);
    }
}

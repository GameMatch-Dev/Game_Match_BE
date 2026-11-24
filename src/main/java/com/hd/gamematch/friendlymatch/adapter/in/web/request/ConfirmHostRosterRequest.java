package com.hd.gamematch.friendlymatch.adapter.in.web.request;

import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public record ConfirmHostRosterRequest(
        @JsonDeserialize(contentUsing = FriendlyMatchIntegerDeserializers.LongValue.class)
        @NotNull List<@NotNull @Positive Long> teamMemberIds
) {

    // 명단 확정 요청과 인증 사용자 및 경기 ID를 Command로 변환한다.
    public ConfirmHostRosterCommand toCommand(Long userId, Long matchId) {
        return ConfirmHostRosterCommand.of(userId, matchId, teamMemberIds);
    }
}

package com.hd.gamematch.friendlymatch.adapter.in.web.request;

import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchCommand;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateFriendlyMatchRequest(
        @JsonDeserialize(using = FriendlyMatchIntegerDeserializers.LongValue.class)
        @NotNull @Positive Long hostGameUserId,
        @JsonDeserialize(using = FriendlyMatchIntegerDeserializers.LongValue.class)
        @NotNull @Positive Long gameId,
        @NotNull LocalDate date,
        @NotNull LocalTime time,
        @JsonDeserialize(using = FriendlyMatchIntegerDeserializers.IntegerValue.class)
        Integer sort,
        @JsonDeserialize(using = FriendlyMatchIntegerDeserializers.IntegerValue.class)
        @NotNull @Positive Integer capacity,
        @NotBlank @Size(max = 500) String comment
) {

    // HTTP JSON 요청값과 JWT에서 얻은 사용자 ID를 친선 경기 생성 Command로 변환한다.
    public CreateFriendlyMatchCommand toCommand(Long userId) {
        return CreateFriendlyMatchCommand.of(
                userId,
                hostGameUserId,
                gameId,
                date,
                time,
                toSort(sort),
                capacity,
                comment
        );
    }

    // API 계약의 숫자 값을 참가 방식으로 명시적으로 변환한다.
    private static FriendlyMatchSort toSort(Integer sort) {
        if (sort == null) {
            throw new IllegalArgumentException("sort는 0 또는 1이어야 합니다.");
        }

        return switch (sort) {
            case 0 -> FriendlyMatchSort.TEAM;
            case 1 -> FriendlyMatchSort.INDIVIDUAL;
            default -> throw new IllegalArgumentException("sort는 0 또는 1이어야 합니다.");
        };
    }
}

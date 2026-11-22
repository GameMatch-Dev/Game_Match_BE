package com.hd.gamematch.friendlymatch.adapter.in.web.response;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.game.adapter.in.web.response.GameResponse;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateFriendlyMatchResponse(
        Long id,
        HostResponse host,
        GameResponse game,
        LocalDate date,
        LocalTime time,
        Integer sort,
        Integer state,
        Integer capacity,
        String comment
) {

    // FriendlyMatch 도메인 객체를 친선 경기 생성 API의 응답 JSON 모양으로 변환한다.
    public static CreateFriendlyMatchResponse from(FriendlyMatch friendlyMatch) {
        return new CreateFriendlyMatchResponse(
                friendlyMatch.id(),
                new HostResponse(
                        friendlyMatch.host().id(),
                        friendlyMatch.host().nickname()
                ),
                GameResponse.from(friendlyMatch.game()),
                friendlyMatch.date(),
                friendlyMatch.time(),
                // 공개 응답 값은 enum 선언 순서와 분리해 명시적으로 매핑한다.
                switch (friendlyMatch.sort()) {
                    case TEAM -> 0;
                    case INDIVIDUAL -> 1;
                },
                switch (friendlyMatch.state()) {
                    case RECRUITING -> 0;
                },
                friendlyMatch.capacity(),
                friendlyMatch.comment()
        );
    }

    public record HostResponse(
            Long id,
            String nickname
    ) {
    }
}

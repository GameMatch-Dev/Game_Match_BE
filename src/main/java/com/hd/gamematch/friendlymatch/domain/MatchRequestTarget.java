package com.hd.gamematch.friendlymatch.domain;

// 참가 신청 가능 여부를 판단하는 데 필요한 경기 정보다.
public record MatchRequestTarget(
        // 신청 대상 경기 식별자다.
        Long matchId,
        // 대상 경기의 게임 식별자다.
        Long gameId,
        // 현재 참가자를 모집 중인지 나타낸다.
        boolean recruiting
) {
}

package com.hd.gamematch.friendlymatch.domain;

import java.util.List;

// 경기 참가 신청의 현재 정보를 전달한다.
public record MatchRequest(
        // 신청 식별자이며 아직 저장하지 않은 신청은 null이다.
        Long id,
        // 신청 대상 경기 식별자다.
        Long matchId,
        // 신청자의 게임 프로필 식별자다.
        Long gameUserId,
        // 함께 신청한 팀원의 게임 프로필 식별자 목록이다.
        List<Long> teamMemberIds,
        // 신청자가 남긴 소개 글이다.
        String comment,
        // 신청의 현재 처리 상태다.
        MatchRequestState state
) {
}

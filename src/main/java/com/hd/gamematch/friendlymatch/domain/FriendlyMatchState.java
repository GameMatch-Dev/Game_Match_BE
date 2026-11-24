package com.hd.gamematch.friendlymatch.domain;

/**
 * 친선 경기 모집의 현재 진행 상태를 구분한다.
 */
public enum FriendlyMatchState {
    RECRUITING,
    // 상대 팀 승인이 완료되었거나 개인전 정원이 찬 모집이다.
    RECRUITMENT_CLOSED
}

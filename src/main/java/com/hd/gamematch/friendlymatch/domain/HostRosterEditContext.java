package com.hd.gamematch.friendlymatch.domain;

// 경기 잠금 안에서 주최 권한과 승인 기록을 함께 판단할 조회 결과다.
public record HostRosterEditContext(FriendlyMatch match, boolean hasApprovedRequests) {
}

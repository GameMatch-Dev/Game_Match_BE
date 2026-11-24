package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;

import java.util.List;

public interface SaveHostRosterPort {

    // 경기의 호스트 팀원 명단을 저장하고 저장된 경기를 반환한다.
    FriendlyMatch saveHostRoster(Long matchId, List<Long> teamMemberIds);
}

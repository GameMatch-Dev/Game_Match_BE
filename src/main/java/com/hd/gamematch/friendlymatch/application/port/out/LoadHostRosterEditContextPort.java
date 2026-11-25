package com.hd.gamematch.friendlymatch.application.port.out;

import com.hd.gamematch.friendlymatch.domain.HostRosterEditContext;
import java.util.Optional;

public interface LoadHostRosterEditContextPort {

    // 승인과 같은 경기 행을 잠근 뒤 명단 수정에 필요한 현재 정보를 조회한다.
    Optional<HostRosterEditContext> loadForUpdate(Long matchId);
}

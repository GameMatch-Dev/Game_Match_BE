package com.hd.gamematch.friendlymatch.application.port.in.hostroster;

import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;

public interface ConfirmHostRosterUseCase {

    // 인증된 호스트의 경기 명단을 확정하고 해당 경기를 반환한다.
    FriendlyMatch confirm(ConfirmHostRosterCommand command);
}

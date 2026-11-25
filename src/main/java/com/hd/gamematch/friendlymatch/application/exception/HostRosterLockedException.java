package com.hd.gamematch.friendlymatch.application.exception;

public class HostRosterLockedException extends RuntimeException {

    // 첫 참가 승인 이후 실제 참가 명단의 변경을 거부한다.
    public HostRosterLockedException() {
        super("참가 신청 승인 후에는 주최 측 명단을 변경할 수 없습니다.");
    }
}

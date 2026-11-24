package com.hd.gamematch.friendlymatch.application.exception;

// 경기 호스트가 아닌 사용자가 호스트 명단 확정을 요청할 때 발생한다.
public class HostRosterAccessDeniedException extends RuntimeException {

    // 호스트 명단 확정 권한이 없는 요청의 업무 메시지를 지정한다.
    public HostRosterAccessDeniedException() {
        super("호스트 명단을 확정할 권한이 없습니다.");
    }
}

package com.hd.gamematch.friendlymatch.application.port.in.approve;

// 참가 신청 승인 기능의 입력 포트다.
public interface ApproveMatchRequestUseCase {

    // 승인 요청을 처리하고 해당 신청의 실제 참가자 정보를 반환한다.
    ApproveMatchRequestResult approve(ApproveMatchRequestCommand command);
}

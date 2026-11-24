package com.hd.gamematch.friendlymatch.application.port.in.request;

// Controller가 참가 신청 기능을 호출하기 위한 입력 포트다.
public interface CreateMatchRequestUseCase {

    // 참가 신청 입력값을 받아 신청 ID와 신규 생성 여부를 반환한다.
    CreateMatchRequestResult createRequest(CreateMatchRequestCommand command);
}

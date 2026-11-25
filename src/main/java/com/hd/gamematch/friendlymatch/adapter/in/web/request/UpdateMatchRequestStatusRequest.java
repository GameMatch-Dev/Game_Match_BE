package com.hd.gamematch.friendlymatch.adapter.in.web.request;

import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestCommand;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record UpdateMatchRequestStatusRequest(@NotNull MatchRequestState status) {

    // 현재 API는 승인만 지원하며 다른 상태 변경 요청은 입력 오류로 거부한다.
    @AssertTrue(message = "참가 신청 상태는 APPROVED만 요청할 수 있습니다.")
    public boolean isApprovalStatus() {
        return status == null || status == MatchRequestState.APPROVED;
    }

    // 검증된 승인 요청에 인증 사용자와 경로의 신청 ID를 결합한다.
    public ApproveMatchRequestCommand toCommand(Long userId, Long requestId) {
        return ApproveMatchRequestCommand.of(userId, requestId);
    }
}

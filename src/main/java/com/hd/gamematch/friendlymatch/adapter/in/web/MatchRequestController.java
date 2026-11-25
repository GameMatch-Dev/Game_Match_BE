package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.adapter.in.web.request.UpdateMatchRequestStatusRequest;
import com.hd.gamematch.friendlymatch.adapter.in.web.response.ApproveMatchRequestResponse;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestUseCase;
import com.hd.gamematch.global.response.CommonResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/match-requests")
@RequiredArgsConstructor
@Validated
public class MatchRequestController {

    private final ApproveMatchRequestUseCase approveMatchRequestUseCase;

    // 인증 사용자의 승인 요청을 UseCase에 전달하고 승인 결과를 반환한다.
    @PatchMapping("/{requestId}/status")
    public ResponseEntity<CommonResponse<ApproveMatchRequestResponse>> updateStatus(
            @PathVariable @Min(1) Long requestId,
            @Valid @RequestBody UpdateMatchRequestStatusRequest request,
            Authentication authentication
    ) {
        // 실제 주최자 권한 확인은 인증 사용자 ID를 받은 Service가 처리한다.
        Long userId = Long.valueOf(authentication.getName());
        ApproveMatchRequestResult result = approveMatchRequestUseCase.approve(
                request.toCommand(userId, requestId));

        return ResponseEntity.ok(CommonResponse.success(ApproveMatchRequestResponse.from(result)));
    }
}

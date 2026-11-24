package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.adapter.in.web.request.CreateFriendlyMatchRequest;
import com.hd.gamematch.friendlymatch.adapter.in.web.request.CreateMatchRequest;
import com.hd.gamematch.friendlymatch.adapter.in.web.request.ConfirmHostRosterRequest;
import com.hd.gamematch.friendlymatch.adapter.in.web.response.CreateFriendlyMatchResponse;
import com.hd.gamematch.friendlymatch.adapter.in.web.response.CreateMatchRequestResponse;
import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchUseCase;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterUseCase;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.global.response.CommonResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 친선 경기 모집 생성과 참가 신청 HTTP 요청의 진입점이다.
 */
@RestController
@RequestMapping("/friendly-matches")
@RequiredArgsConstructor
@Validated
public class FriendlyMatchController {

    private final CreateFriendlyMatchUseCase createFriendlyMatchUseCase;
    private final CreateMatchRequestUseCase createMatchRequestUseCase;
    private final ConfirmHostRosterUseCase confirmHostRosterUseCase;

    // 인증된 사용자가 자신의 게임 프로필을 호스트로 친선 경기 모집을 생성한다.
    @PostMapping
    public ResponseEntity<CommonResponse<CreateFriendlyMatchResponse>> createFriendlyMatch(
            @Valid @RequestBody CreateFriendlyMatchRequest request,
            Authentication authentication
    ) {
        // Spring JWT 인증의 name에서 내부 사용자 ID를 읽는다.
        Long userId = Long.valueOf(authentication.getName());

        // HTTP 요청을 UseCase 입력값으로 변환하는 책임은 Request DTO에 둔다.
        FriendlyMatch friendlyMatch = createFriendlyMatchUseCase.create(request.toCommand(userId));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(CreateFriendlyMatchResponse.from(friendlyMatch)));
    }

    // 인증된 호스트의 명단 확정 요청을 전달하고 경기 정보를 반환한다.
    @PutMapping("/{matchId}/host-roster")
    public ResponseEntity<CommonResponse<CreateFriendlyMatchResponse>> confirmHostRoster(
            @PathVariable @Min(1) Long matchId,
            @Valid @RequestBody ConfirmHostRosterRequest request,
            Authentication authentication
    ) {
        // 인증 사용자와 경로의 경기 ID를 명단 확정 입력에 함께 전달한다.
        Long userId = Long.valueOf(authentication.getName());
        FriendlyMatch friendlyMatch = confirmHostRosterUseCase.confirm(request.toCommand(userId, matchId));

        return ResponseEntity.ok(CommonResponse.success(CreateFriendlyMatchResponse.from(friendlyMatch)));
    }

    // 인증된 대표 사용자의 참가 신청을 전달하고 생성 여부에 맞는 상태로 응답한다.
    @PostMapping("/{matchId}/requests")
    public ResponseEntity<CommonResponse<CreateMatchRequestResponse>> createMatchRequest(
            @PathVariable @Min(1) Long matchId,
            @Valid @RequestBody CreateMatchRequest request,
            Authentication authentication
    ) {
        // JWT의 내부 사용자 ID와 HTTP 요청값을 신청 Command로 묶는다.
        Long userId = Long.valueOf(authentication.getName());
        CreateMatchRequestResult result = createMatchRequestUseCase.createRequest(
                request.toCommand(userId, matchId)
        );

        // 신규 신청과 기존 신청의 재활성화를 UseCase 결과로 구분한다.
        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CommonResponse.success(CreateMatchRequestResponse.from(result)));
    }
}

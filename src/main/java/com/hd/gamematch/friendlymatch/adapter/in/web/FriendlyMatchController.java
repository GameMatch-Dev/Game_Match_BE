package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.adapter.in.web.request.CreateFriendlyMatchRequest;
import com.hd.gamematch.friendlymatch.adapter.in.web.response.CreateFriendlyMatchResponse;
import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchUseCase;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 친선 경기 모집 생성 HTTP 요청의 진입점이다.
 */
@RestController
@RequestMapping("/friendly-matches")
@RequiredArgsConstructor
public class FriendlyMatchController {

    private final CreateFriendlyMatchUseCase createFriendlyMatchUseCase;

    // 인증된 사용자가 자신의 게임 프로필을 호스트로 친선 경기 모집을 생성한다.
    @PostMapping
    public ResponseEntity<CommonResponse<CreateFriendlyMatchResponse>> createFriendlyMatch(
            @RequestBody CreateFriendlyMatchRequest request,
            Authentication authentication
    ) {
        // JWT Filter가 검증한 토큰의 sub를 현재 내부 사용자 ID로 넣어 둔다.
        Long userId = (Long) authentication.getPrincipal();

        // HTTP 요청을 UseCase 입력값으로 변환하는 책임은 Request DTO에 둔다.
        FriendlyMatch friendlyMatch = createFriendlyMatchUseCase.create(request.toCommand(userId));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(CreateFriendlyMatchResponse.from(friendlyMatch)));
    }
}

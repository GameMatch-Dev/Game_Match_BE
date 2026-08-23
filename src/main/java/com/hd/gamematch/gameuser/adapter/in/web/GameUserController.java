package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.game.application.port.in.FindGameQuery;
import com.hd.gamematch.game.application.port.in.FindGameUseCase;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserCommand;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserUseCase;
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
 * 게임 프로필 등록 HTTP 요청의 진입점이다.
 *
 * <p>HTTP 입력과 인증된 사용자를 읽어 Command를 만들고, 게임 프로필 등록 UseCase를 호출한다.
 * 등록된 게임 프로필과 연결된 게임 정보를 201 Created 응답으로 반환한다.</p>
 */
@RestController
@RequestMapping("/game-users")
@RequiredArgsConstructor
public class GameUserController {

    private final FindGameUseCase findGameUseCase;

    private final RegisterGameUserUseCase registerGameUserUseCase;
    @PostMapping
    public ResponseEntity<CommonResponse<RegisterGameUserResponse>> postUserProfile(
            @RequestBody RegisterGameUserRequest request,
            Authentication authentication
    ) {
        // 클라이언트가 JSON으로 보낸 게임과 닉네임이다.
        Long gameId = request.gameId();
        String nickname = request.nickname();

        // JWT Filter가 검증한 토큰의 sub를 현재 내부 사용자 ID로 넣어 둔다.
        Long userId = (Long) authentication.getPrincipal();


        // 등록 전에 게임 상세 정보를 조회한다.
        Game game = findGameUseCase.findGame(FindGameQuery.of(gameId));

        // 게임 프로필 등록 UseCase에 전달할 기능 입력값이다.
        RegisterGameUserCommand command = new RegisterGameUserCommand(
                userId,
                gameId,
                nickname
        );

        // DB가 생성한 게임 프로필 ID를 받는다.
        Long gameUserId = registerGameUserUseCase.register(command);

        RegisterGameUserResponse response = RegisterGameUserResponse.of(
                gameUserId,
                nickname,
                game,
                userId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(response));
    }
}

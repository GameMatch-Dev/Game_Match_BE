package com.hd.gamematch.gameuser.adapter.in.web;

import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 프로필 등록 HTTP 요청의 진입점이다.
 *
 * <p>현재 단계는 HTTP 입력과 인증된 사용자를 읽어 Command로 만드는 데까지만 다룬다.
 * 다음 단계에서 UseCase를 주입하고 Command를 전달한다.</p>
 */
@RestController
@RequestMapping("/game-users")
@RequiredArgsConstructor
public class GameUserController {

    @PostMapping
    public void postUserProfile(
            @RequestBody RegisterGameUserRequest request,
            Authentication authentication
    ) {
        // 클라이언트가 JSON으로 보낸 게임과 닉네임이다.
        Long gameId = request.gameId();
        String nickname = request.nickname();

        // JWT Filter가 검증한 토큰의 sub를 현재 내부 사용자 ID로 넣어 둔다.
        Long userId = (Long) authentication.getPrincipal();

        // 다음 단계에서 RegisterGameUserUseCase에 전달할 기능 입력값이다.
        RegisterGameUserCommand command = new RegisterGameUserCommand(
                userId,
                gameId,
                nickname
        );
    }
}

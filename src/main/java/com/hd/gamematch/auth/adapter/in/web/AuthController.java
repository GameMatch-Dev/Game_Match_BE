package com.hd.gamematch.auth.adapter.in.web;

import com.hd.gamematch.auth.application.service.AuthService;
import com.hd.gamematch.auth.application.exception.InvalidAuthRequestException;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 표준 OAuth2 Client authorization endpoint로 넘긴다.
     * state 생성·저장과 카카오 authorization URL 조립은 Spring Security가 처리한다.
     */
    @GetMapping("/kakao/authorize")
    public ResponseEntity<Void> authorizeKakaoLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/oauth2/authorization/kakao")
                .build();
    }

    @PostMapping("/token")
    public CommonResponse<TokenResponse> exchangeLoginTicket(@RequestBody(required = false) LoginTicketRequest request) {
        if (request == null || !StringUtils.hasText(request.ticket())) {
            throw new InvalidAuthRequestException();
        }
        JwtTokenService.IssuedAccessToken accessToken = authService.exchangeLoginTicket(request.ticket());
        return CommonResponse.success(new TokenResponse(
                accessToken.value(),
                "Bearer",
                accessToken.expiresInSeconds()
        ));
    }

    @GetMapping("/me")
    public CommonResponse<CurrentUserResponse> me(Authentication authentication) {
        // JwtAuthenticationFilter가 JWT의 sub(내부 userId)를 principal에 넣어 준다.
        Long userId = (Long) authentication.getPrincipal();
        return CommonResponse.success(new CurrentUserResponse(userId));
    }

    public record LoginTicketRequest(String ticket) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
    }

    public record CurrentUserResponse(Long userId) {
    }
}

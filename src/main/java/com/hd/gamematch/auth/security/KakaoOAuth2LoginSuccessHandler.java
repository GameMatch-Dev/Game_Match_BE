package com.hd.gamematch.auth.security;

import com.hd.gamematch.auth.application.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class KakaoOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final KakaoOAuth2LoginFailureHandler kakaoOAuth2LoginFailureHandler;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        Object kakaoMemberId = oauth2Authentication.getPrincipal().getAttribute("id");
        if (kakaoMemberId == null) {
            kakaoOAuth2LoginFailureHandler.redirectToFrontend(
                    response,
                    com.hd.gamematch.global.exception.ErrorCode.AUTH_PROVIDER_UNAVAILABLE
            );
            return;
        }

        URI frontendRedirectUri = authService.completeKakaoLogin(String.valueOf(kakaoMemberId));
        // OAuth2 state를 보관하던 임시 세션은 로그인 완료 후 유지하지 않는다.
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(frontendRedirectUri.toString());
    }
}

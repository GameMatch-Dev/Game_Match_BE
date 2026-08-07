package com.hd.gamematch.auth.security;

import com.hd.gamematch.auth.config.AuthLoginProperties;
import com.hd.gamematch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class KakaoOAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final AuthLoginProperties authLoginProperties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        redirectToFrontend(response, classify(exception));
    }

    public void redirectToFrontend(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        // Kakao가 보낸 상세 오류·인가 코드·토큰은 프런트 URL이나 응답에 노출하지 않는다.
        URI redirectUri = UriComponentsBuilder.fromUriString(authLoginProperties.frontendRedirectUri())
                .replaceQueryParam("error", errorCode.code())
                .build(true)
                .toUri();
        response.sendRedirect(redirectUri.toString());
    }

    private ErrorCode classify(AuthenticationException exception) {
        if (hasProviderUnavailableCause(exception)) {
            return ErrorCode.AUTH_PROVIDER_UNAVAILABLE;
        }
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String errorCode = oauth2Exception.getError().getErrorCode();
            if ("invalid_request".equals(errorCode)) {
                return ErrorCode.AUTH_400;
            }
            if ("invalid_token_response".equals(errorCode)
                    || "server_error".equals(errorCode)
                    || "temporarily_unavailable".equals(errorCode)) {
                return ErrorCode.AUTH_PROVIDER_UNAVAILABLE;
            }
        }
        // state 불일치, 사용자 취소, 만료·재사용된 인가 코드는 모두 로그인 실패로만 안내한다.
        return ErrorCode.AUTH_401;
    }

    private boolean hasProviderUnavailableCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceAccessException || current instanceof HttpServerErrorException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

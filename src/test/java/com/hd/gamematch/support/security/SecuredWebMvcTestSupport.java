package com.hd.gamematch.support.security;

import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.auth.security.KakaoOAuth2LoginFailureHandler;
import com.hd.gamematch.auth.security.KakaoOAuth2LoginSuccessHandler;
import com.hd.gamematch.auth.security.RestAuthenticationEntryPoint;
import com.hd.gamematch.auth.security.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Objects;

import static org.mockito.BDDMockito.given;

/**
 * 인증이 필요한 {@code @WebMvcTest}가 운영 보안 설정과 JWT 인증 흐름을 함께 검증하도록 돕는다.
 */
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
public abstract class SecuredWebMvcTestSupport {

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private KakaoOAuth2LoginSuccessHandler kakaoOAuth2LoginSuccessHandler;

    @MockitoBean
    private KakaoOAuth2LoginFailureHandler kakaoOAuth2LoginFailureHandler;

    /**
     * 지정한 내부 사용자 ID를 JWT subject로 해석하도록 decoder mock을 설정하고 Authorization 헤더 값을 반환한다.
     */
    protected String bearerTokenFor(Long userId) {
        Objects.requireNonNull(userId, "userId는 필수입니다.");

        String tokenValue = "test-token-" + userId;
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .subject(String.valueOf(userId))
                .build();

        given(jwtTokenService.jwtDecoder()).willReturn(jwtDecoder);
        given(jwtDecoder.decode(tokenValue)).willReturn(jwt);

        return "Bearer " + tokenValue;
    }
}

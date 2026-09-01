package com.hd.gamematch.support.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hd.gamematch.auth.security.JwtAuthenticationFilter;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.auth.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Objects;

import static org.mockito.BDDMockito.given;

/**
 * 인증이 필요한 {@code @WebMvcTest}가 운영 보안 설정과 JWT 인증 흐름을 함께 검증하도록 돕는다.
 */
@Import({
        SecuredWebMvcTestSupport.SecurityTestConfiguration.class
})
public abstract class SecuredWebMvcTestSupport {

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // 지정한 내부 사용자 ID를 JWT subject로 해석하도록 decoder mock을 설정하고 Authorization 헤더 값을 반환한다.

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

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class SecurityTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new RestAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                JwtTokenService jwtTokenService,
                RestAuthenticationEntryPoint restAuthenticationEntryPoint
        ) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(HttpMethod.GET, "/game-users/*").permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(
                            new JwtAuthenticationFilter(jwtTokenService),
                            UsernamePasswordAuthenticationFilter.class
                    );
            return http.build();
        }
    }
}

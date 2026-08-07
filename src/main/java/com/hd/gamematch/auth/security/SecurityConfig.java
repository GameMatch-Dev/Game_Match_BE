package com.hd.gamematch.auth.security;

import com.hd.gamematch.auth.config.AuthLoginProperties;
import com.hd.gamematch.auth.config.AuthCorsProperties;
import com.hd.gamematch.auth.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({AuthLoginProperties.class, AuthCorsProperties.class, JwtProperties.class})
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final KakaoOAuth2LoginSuccessHandler kakaoOAuth2LoginSuccessHandler;
    private final KakaoOAuth2LoginFailureHandler kakaoOAuth2LoginFailureHandler;

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthCorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.origins());
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        // Bearer JWT를 헤더로 보내므로 쿠키 credential과 wildcard origin을 함께 허용하지 않는다.
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // OAuth2 state를 callback까지 보관할 때만 HttpSession을 잠시 사용한다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/auth/kakao/authorize", "/auth/kakao/callback").permitAll()
                        .requestMatchers("/oauth2/authorization/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/token").permitAll()
                        // 기존 게임 조회 계약은 이번 최소 인증 작업에서도 공개 API로 유지한다.
                        .requestMatchers(HttpMethod.GET, "/games", "/games/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Kakao 환경 변수가 있는 실행 환경에서만 OAuth2 Client 필터를 구성한다.
        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .redirectionEndpoint(endpoint -> endpoint.baseUri("/auth/kakao/callback"))
                    .successHandler(kakaoOAuth2LoginSuccessHandler)
                    .failureHandler(kakaoOAuth2LoginFailureHandler)
            );
        }
        return http.build();
    }
}

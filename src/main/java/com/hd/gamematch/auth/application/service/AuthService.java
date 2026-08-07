package com.hd.gamematch.auth.application.service;

import com.hd.gamematch.auth.config.AuthLoginProperties;
import com.hd.gamematch.auth.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthLoginProperties authLoginProperties;
    private final LoginIdentityService loginIdentityService;
    private final LoginTicketService loginTicketService;
    private final JwtTokenService jwtTokenService;

    /**
     * 카카오 access token 교환과 state 검증은 Spring Security OAuth2 Client가 담당한다.
     * 이 서비스는 검증된 카카오 회원번호를 내부 userId에 연결하는 일만 맡는다.
     */
    public URI completeKakaoLogin(String kakaoMemberId) {
        Long userId = loginIdentityService.findOrCreateKakaoUser(kakaoMemberId);
        String ticket = loginTicketService.issue(userId);

        return UriComponentsBuilder.fromUriString(authLoginProperties.frontendRedirectUri())
                .queryParam("ticket", ticket)
                .build(true)
                .toUri();
    }

    public JwtTokenService.IssuedAccessToken exchangeLoginTicket(String ticket) {
        Long userId = loginTicketService.consume(ticket);
        return jwtTokenService.issueAccessToken(userId);
    }
}

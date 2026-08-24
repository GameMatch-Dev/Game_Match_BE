package com.hd.gamematch.auth.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class JwtAuthenticationFilterTest {

    private final JwtTokenService jwtTokenService = org.mockito.Mockito.mock(JwtTokenService.class);
    private final JwtDecoder jwtDecoder = org.mockito.Mockito.mock(JwtDecoder.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 형식이_잘못된_베어러_헤더는_인증하지_않는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Authorization", "Basic credentials");
        AtomicBoolean nextFilterCalled = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), markFilterChain(nextFilterCalled));

        assertTrue(nextFilterCalled.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        then(jwtTokenService).shouldHaveNoInteractions();
    }

    @Test
    void 베어러_토큰을_해독할_수_없으면_인증_정보를_비운다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Authorization", "Bearer invalid-token");
        given(jwtTokenService.jwtDecoder()).willReturn(jwtDecoder);
        given(jwtDecoder.decode("invalid-token")).willThrow(new BadJwtException("invalid token"));
        AtomicBoolean nextFilterCalled = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), markFilterChain(nextFilterCalled));

        assertTrue(nextFilterCalled.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void 유효한_베어러_토큰의_내부_사용자_식별자를_주체에_저장한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Authorization", "Bearer valid-token");
        given(jwtTokenService.jwtDecoder()).willReturn(jwtDecoder);
        given(jwtDecoder.decode("valid-token")).willReturn(Jwt.withTokenValue("valid-token")
                .header("alg", "HS256")
                .subject("7")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build());
        AtomicBoolean nextFilterCalled = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), markFilterChain(nextFilterCalled));

        assertTrue(nextFilterCalled.get());
        assertEquals(7L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    private FilterChain markFilterChain(AtomicBoolean nextFilterCalled) {
        return (request, response) -> nextFilterCalled.set(true);
    }
}

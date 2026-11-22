package com.hd.gamematch.auth.security;

import com.hd.gamematch.auth.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtResourceServerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired JwtProperties jwtProperties;

    // 실제 서명된 토큰의 sub가 기본 JWT 인증을 거쳐 Controller에 전달된다.
    @Test
    void 실제_JWT로_현재_사용자를_조회한다() throws Exception {
        String token = jwtTokenService.issueAccessToken(7L).value();
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7));
    }

    // 정상 토큰의 서명을 변조해도 인증을 통과할 수 없다.
    @Test
    void 서명을_변조하면_401을_반환한다() throws Exception {
        String token = jwtTokenService.issueAccessToken(7L).value();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // 정상 키와 issuer를 사용해도 허용 오차보다 충분히 오래전에 만료된 토큰은 거부한다.
    @Test
    void 만료된_JWT면_401을_반환한다() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject("7")
                .issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(300))
                .build();

        // 운영과 동일한 키로 서명하고, 발급 시각은 만료 시각보다 앞서도록 유지한다.
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // 서명과 시간이 정상이더라도 설정된 발급자와 다르면 인증을 거부한다.
    @Test
    void issuer가_다른_JWT면_401을_반환한다() throws Exception {
        JwtTokenService otherIssuer = new JwtTokenService(new JwtProperties(
                jwtProperties.secret(),
                jwtProperties.issuer() + "-other",
                jwtProperties.accessTokenTtl()
        ));
        String token = otherIssuer.issueAccessToken(7L).value();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // 토큰이 없으면 보호 API는 기존 공통 401 응답을 유지한다.
    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // Spring Bearer 필터의 인증 실패도 JSON entry point로 연결한다.
    @Test
    void 잘못된_토큰이면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // 공개 API는 토큰 없이 접근할 수 있다.
    @Test
    void 공개_API는_토큰이_없어도_통과한다() throws Exception {
        mockMvc.perform(get("/games")).andExpect(status().isOk());
    }

    // 공개 경로라도 Bearer 인증을 시도하면 유효하지 않은 토큰은 거부한다.
    @Test
    void 공개_API에_잘못된_Bearer를_보내면_401이다() throws Exception {
        mockMvc.perform(get("/games").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    // 서명이 유효해도 사용자 ID로 해석할 수 없는 subject는 거부한다.
    @Test
    void 숫자가_아닌_subject는_인증에_실패한다() throws Exception {
        String token = jwtTokenService.issueAccessToken(null).value();
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }
}

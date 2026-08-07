package com.hd.gamematch.auth.security;

import com.hd.gamematch.auth.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtProperties properties;

    public IssuedAccessToken issueAccessToken(Long userId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(userId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        String accessToken = jwtEncoder().encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedAccessToken(accessToken, expiresAt.getEpochSecond() - issuedAt.getEpochSecond());
    }

    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    private JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey()));
    }

    private SecretKey secretKey() {
        // HMAC 키는 최소 256bit가 필요하므로 설정 문자열을 SHA-256으로 고정 길이 키로 만든다.
        byte[] source = properties.secret().getBytes(StandardCharsets.UTF_8);
        try {
            byte[] key = java.security.MessageDigest.getInstance("SHA-256").digest(source);
            return new SecretKeySpec(key, "HmacSHA256");
        } catch (java.security.NoSuchAlgorithmException exception) {
            // Java 표준 알고리즘이므로 발생하면 실행 환경 자체가 비정상인 경우다.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    public record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}

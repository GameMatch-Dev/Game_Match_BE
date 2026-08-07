package com.hd.gamematch.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl
) {
}

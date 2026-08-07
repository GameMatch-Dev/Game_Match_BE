package com.hd.gamematch.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.login")
public record AuthLoginProperties(String frontendRedirectUri) {
}

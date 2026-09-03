package com.hd.gamematch.auth.adapter.in.web.response;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}

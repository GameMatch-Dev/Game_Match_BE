package com.hd.gamematch.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hd.gamematch.global.response.CommonResponse;
import com.hd.gamematch.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException {
        response.setStatus(ErrorCode.AUTH_401.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new CommonResponse<>(
                        false,
                        ErrorCode.AUTH_401.code(),
                        ErrorCode.AUTH_401.defaultMessage(),
                        null
                )
        );
    }
}

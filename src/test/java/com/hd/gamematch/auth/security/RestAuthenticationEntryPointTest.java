package com.hd.gamematch.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class RestAuthenticationEntryPointTest {

    @Test
    void 인증되지_않은_요청에_인증_401_공통_응답을_반환한다() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest("GET", "/auth/me"),
                response,
                new InsufficientAuthenticationException("authentication is required")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertFalse((Boolean) body.get("success"));
        assertEquals("AUTH_401", body.get("code"));
        assertNull(body.get("data"));
    }
}

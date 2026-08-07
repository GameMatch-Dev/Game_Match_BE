package com.hd.gamematch.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
class CorsSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void loadsConfiguredFrontendOriginIntoCorsSource() {
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/auth/me")
        );
        assertEquals(java.util.List.of("http://localhost:3000"), configuration.getAllowedOrigins());
    }

    @Test
    void allowsConfiguredFrontendOriginToPreflightAuthenticatedRequest() throws Exception {
        mockMvc.perform(options("/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }
}

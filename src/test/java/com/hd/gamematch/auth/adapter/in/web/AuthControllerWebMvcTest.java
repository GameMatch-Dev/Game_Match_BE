package com.hd.gamematch.auth.adapter.in.web;

import com.hd.gamematch.auth.application.exception.InvalidLoginTicketException;
import com.hd.gamematch.auth.application.service.AuthService;
import com.hd.gamematch.auth.adapter.in.web.exception.AuthExceptionHandler;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerWebMvcTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = org.mockito.Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler(), new AuthExceptionHandler())
                .build();
    }

    @Test
    void 로그인_티켓_요청_본문이_없으면_인증_400을_반환한다() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_400"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void 로그인_티켓이_비어_있으면_인증_400을_반환한다() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\" \"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_400"));
    }

    @Test
    void 로그인_티켓이_유효하지_않으면_인증_401을_반환한다() throws Exception {
        given(authService.exchangeLoginTicket("expired-ticket"))
                .willThrow(new InvalidLoginTicketException());

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"expired-ticket\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void 로그인_티켓이_유효하면_액세스_토큰을_반환한다() throws Exception {
        given(authService.exchangeLoginTicket("valid-ticket"))
                .willReturn(new JwtTokenService.IssuedAccessToken("access-token", 900));

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"valid-ticket\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(900));
    }
}

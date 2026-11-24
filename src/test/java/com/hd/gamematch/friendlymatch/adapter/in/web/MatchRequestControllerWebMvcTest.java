package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import com.hd.gamematch.support.security.SecuredWebMvcTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = MatchRequestController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class MatchRequestControllerWebMvcTest extends SecuredWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApproveMatchRequestUseCase approveMatchRequestUseCase;

    // 승인 요청과 응답의 변환을 검증한다. 실제 주최자 판별은 Mock 뒤의 Service 검증 범위다.
    @Test
    void 인증된_사용자가_참가_신청을_승인하면_200을_반환한다() throws Exception {
        // given: 인증 사용자 3번이 신청 1번을 승인했을 때의 업무 결과를 준비한다.
        ApproveMatchRequestCommand command = ApproveMatchRequestCommand.of(3L, 1L);
        ApproveMatchRequestResult result = ApproveMatchRequestResult.of(
                1L,
                100L,
                MatchRequestState.APPROVED,
                List.of(20L, 21L)
        );
        given(approveMatchRequestUseCase.approve(command)).willReturn(result);

        // when: JWT와 경로의 신청 ID, 승인 상태를 HTTP 요청으로 전달한다.
        mockMvc.perform(patch("/match-requests/{requestId}/status", 1L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                // then: 공통 응답과 해당 신청의 실제 참가자 목록을 확인한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(1L))
                .andExpect(jsonPath("$.data.matchId").value(100L))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.participantGameUserIds.length()").value(2))
                .andExpect(jsonPath("$.data.participantGameUserIds[0]").value(20L))
                .andExpect(jsonPath("$.data.participantGameUserIds[1]").value(21L));

        // JWT 사용자 ID와 신청 ID가 UseCase의 입력으로 전달됐는지 확인한다.
        then(approveMatchRequestUseCase).should().approve(command);
    }
}

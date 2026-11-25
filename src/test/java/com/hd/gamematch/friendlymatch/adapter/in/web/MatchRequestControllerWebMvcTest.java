package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import com.hd.gamematch.support.security.SecuredWebMvcTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchApprovalAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.MatchApprovalConflictException;
import com.hd.gamematch.friendlymatch.application.exception.MatchNotRecruitingException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestNotFoundException;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
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
import static org.mockito.Mockito.verifyNoInteractions;
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

    // 인증 없는 승인 요청은 업무 로직에 도달하지 않는다.
    @Test
    void 인증이_없으면_401이다() throws Exception {
        mockMvc.perform(patch("/match-requests/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(approveMatchRequestUseCase);
    }

    // 공개 입력 계약에 맞지 않는 상태는 승인 UseCase로 전달하지 않는다.
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}",
            "{\"status\":\"PENDING\"}", "{\"status\":\"REJECTED\"}", ""})
    void 잘못된_상태_본문은_400이다(String body) throws Exception {
        mockMvc.perform(patch("/match-requests/1/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(approveMatchRequestUseCase);
    }

    // 신청 식별자는 양의 정수로 제한한다.
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "abc"})
    void 잘못된_신청_ID는_400이다(String id) throws Exception {
        mockMvc.perform(patch("/match-requests/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(approveMatchRequestUseCase);
    }

    // 실제 권한 검증은 Service가 수행하고 Controller는 그 예외를 403으로 변환한다.
    @Test
    void 승인_권한_예외는_403이다() throws Exception {
        assertBusinessError(new MatchApprovalAccessDeniedException(), 403, "AUTH_403");
    }

    // 신청이 없다는 업무 예외를 고유 코드와 404로 응답한다.
    @Test
    void 신청_미존재는_404이다() throws Exception {
        assertBusinessError(new MatchRequestNotFoundException(), 404, "MATCH_REQUEST_001");
    }

    // 연결 경기와 참가 프로필의 미존재 오류는 기존 코드를 유지한다.
    @Test
    void 경기_미존재는_404이다() throws Exception {
        assertBusinessError(new FriendlyMatchNotFoundException(), 404, "FRIENDLY_MATCH_001");
    }

    // 승인 시 참가 프로필이 없어지면 기존 프로필 미존재 코드로 응답한다.
    @Test
    void 프로필_미존재는_404이다() throws Exception {
        assertBusinessError(new GameUserNotFoundException(), 404, "GAME_USER_001");
    }

    // 정원과 명단 및 상태 충돌은 업무 사유를 보존한 409 응답이다.
    @Test
    void 승인_충돌은_409이다() throws Exception {
        assertBusinessError(new MatchApprovalConflictException("경기 참가자는 중복될 수 없습니다."),
                409, "MATCH_REQUEST_004");
    }

    // 모집 종료 오류는 기존 신청 API의 409 코드와 호환된다.
    @Test
    void 모집_종료는_409이다() throws Exception {
        assertBusinessError(new MatchNotRecruitingException(), 409, "MATCH_REQUEST_003");
    }

    // Mock 업무 예외가 공통 실패 응답의 상태와 코드 및 메시지로 변환되는지 확인한다.
    private void assertBusinessError(RuntimeException exception, int expectedStatus, String code) throws Exception {
        given(approveMatchRequestUseCase.approve(ApproveMatchRequestCommand.of(3L, 1L)))
                .willThrow(exception);
        mockMvc.perform(patch("/match-requests/1/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));
    }

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

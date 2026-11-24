package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchNotRecruitingException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAlreadyActiveException;
import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchUseCase;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestUseCase;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import com.hd.gamematch.support.security.SecuredWebMvcTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FriendlyMatchController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class CreateMatchRequestControllerWebMvcTest extends SecuredWebMvcTestSupport {

    private static final String REQUEST_BODY = """
            {
              "gameUserId": 10,
              "teamMemberIds": [20, 21],
              "comment": "함께 참가하고 싶습니다."
            }
            """;

    private static final CreateMatchRequestCommand COMMAND = CreateMatchRequestCommand.of(
            3L, 100L, 10L, List.of(20L, 21L), "함께 참가하고 싶습니다."
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateFriendlyMatchUseCase createFriendlyMatchUseCase;

    @MockitoBean
    private CreateMatchRequestUseCase createMatchRequestUseCase;

    // 대표 신청자의 JWT와 팀원 프로필 목록을 전달하고 신규 신청 ID를 응답한다.
    @Test
    void 인증된_대표가_팀원과_신규_신청하면_201을_반환한다() throws Exception {
        // given: 신규 신청 결과를 정확한 Command에 연결한다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willReturn(CreateMatchRequestResult.created(1000L));

        // when: 대표 사용자가 자신의 프로필과 다른 팀원의 프로필로 신청한다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 신규 신청을 공통 성공 응답으로 반환한다.
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(1000L));

        then(createMatchRequestUseCase).should().createRequest(COMMAND);
        then(createMatchRequestUseCase).shouldHaveNoMoreInteractions();
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 재활성화 결과는 기존 신청 ID를 유지하고 HTTP 200으로 응답한다.
    @Test
    void 기존_신청을_재활성화하면_같은_신청_ID와_200을_반환한다() throws Exception {
        // given: UseCase가 기존 1000번 신청을 재활성화한 결과다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willReturn(CreateMatchRequestResult.reactivated(1000L));

        // when: 신규 신청과 동일한 HTTP 요청을 보낸다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: Controller는 UseCase 결과에 따라 상태와 신청 ID를 응답한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(1000L));

        then(createMatchRequestUseCase).should().createRequest(COMMAND);
        then(createMatchRequestUseCase).shouldHaveNoMoreInteractions();
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // JWT가 없는 요청은 신청 작업 전에 인증 오류로 거부한다.
    @Test
    void JWT가_없으면_401을_반환한다() throws Exception {
        // given: 요청 본문은 정상이지만 인증 헤더를 생략한다.
        // when: 인증 없이 신청 API를 호출한다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 인증 실패를 공통 응답으로 반환하고 UseCase는 호출하지 않는다.
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        verifyNoInteractions(createMatchRequestUseCase, createFriendlyMatchUseCase);
    }

    // 대표 신청자 프로필의 소유권 오류를 403으로 변환한다.
    @Test
    void 신청자_프로필이_타인_소유이면_403을_반환한다() throws Exception {
        // given: 소유권 판단은 UseCase가 수행하고 Controller에는 예외만 전달한다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willThrow(new MatchRequestAccessDeniedException());

        // when: 정상 형식의 신청 요청을 보낸다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 권한 오류를 공통 응답으로 변환한다.
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    // 존재하지 않는 게임 프로필 예외는 기존 오류 코드를 유지한다.
    @Test
    void 게임_프로필이_없으면_404를_반환한다() throws Exception {
        // given: UseCase가 요청된 게임 프로필을 찾지 못했다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willThrow(new GameUserNotFoundException());

        // when: 정상 형식의 신청 요청을 보낸다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 게임 프로필 없음 예외를 공통 응답으로 변환한다.
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_USER_001"));
    }

    // 존재하지 않는 친선 경기 예외는 전용 오류 코드로 응답한다.
    @Test
    void 친선_경기가_없으면_404를_반환한다() throws Exception {
        // given: UseCase가 대상 친선 경기를 찾지 못했다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willThrow(new FriendlyMatchNotFoundException());

        // when: 정상 형식의 신청 요청을 보낸다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 친선 경기 없음 예외를 공통 응답으로 변환한다.
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FRIENDLY_MATCH_001"));
    }

    // 이미 활성 상태인 신청 예외를 중복 신청 오류로 변환한다.
    @Test
    void 활성_신청이_이미_있으면_409를_반환한다() throws Exception {
        // given: 중복 여부는 UseCase가 판단하고 전용 예외를 전달한다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willThrow(new MatchRequestAlreadyActiveException());

        // when: 정상 형식의 신청 요청을 보낸다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 중복 신청 예외를 공통 응답으로 변환한다.
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_002"));
    }

    // 모집 중이 아닌 경기의 신청 예외를 상태 충돌 오류로 변환한다.
    @Test
    void 모집이_종료되었으면_409를_반환한다() throws Exception {
        // given: 모집 상태는 UseCase가 판단하고 전용 예외를 전달한다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willThrow(new MatchNotRecruitingException());

        // when: 정상 형식의 신청 요청을 보낸다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 모집 종료 예외를 공통 응답으로 변환한다.
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MATCH_REQUEST_003"));
    }

    // 경기 ID의 형식 오류와 0 또는 음수는 입력 단계에서 거부한다.
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "0", "-1"})
    void 경기_ID가_잘못되면_400을_반환한다(String matchId) throws Exception {
        // given: 본문과 인증은 정상이고 경로 ID만 잘못된 값이다.
        // when: 각 잘못된 경기 ID로 신청한다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", matchId)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                // then: 잘못된 ID를 UseCase에 전달하지 않는다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        verifyNoInteractions(createMatchRequestUseCase, createFriendlyMatchUseCase);
    }

    // 신청자 프로필 ID 누락과 null 또는 잘못된 숫자를 입력 단계에서 거부한다.
    @ParameterizedTest
    @ValueSource(strings = {"", "\"gameUserId\": null,", "\"gameUserId\": 0,",
            "\"gameUserId\": -1,", "\"gameUserId\": \"invalid\","})
    void 신청자_프로필_ID가_없거나_잘못되면_400을_반환한다(String gameUserIdField) throws Exception {
        // given: 각 입력마다 신청자 프로필 ID 필드만 바꾼다.
        String body = """
                {
                  %s
                  "teamMemberIds": [20, 21],
                  "comment": "함께 참가하고 싶습니다."
                }
                """.formatted(gameUserIdField);

        // when: 정상 인증으로 잘못된 프로필 ID를 전송한다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // then: 입력 오류를 공통 응답으로 반환하고 UseCase는 호출하지 않는다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        verifyNoInteractions(createMatchRequestUseCase, createFriendlyMatchUseCase);
    }

    // 신청자 프로필 ID의 소수 부분을 버려 정상 ID로 처리하지 않는다.
    @Test
    void 신청자_프로필_ID가_소수이면_400을_반환한다() throws Exception {
        // given: 소수가 잘리면 정상 Command와 일치해 201이 반환되도록 준비한다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willReturn(CreateMatchRequestResult.created(1000L));
        String body = """
                {
                  "gameUserId": 10.5,
                  "teamMemberIds": [20, 21],
                  "comment": "함께 참가하고 싶습니다."
                }
                """;

        // when: 정상 인증으로 소수 형태의 신청자 프로필 ID를 전송한다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // then: 정수 ID로 변환하지 않고 입력 오류로 거부한다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        verifyNoInteractions(createMatchRequestUseCase, createFriendlyMatchUseCase);
    }

    // 팀원 프로필 ID의 소수 부분을 버려 다른 프로필로 처리하지 않는다.
    @Test
    void 팀원_프로필_ID가_소수이면_400을_반환한다() throws Exception {
        // given: 소수가 잘리면 정상 Command와 일치해 201이 반환되도록 준비한다.
        given(createMatchRequestUseCase.createRequest(COMMAND))
                .willReturn(CreateMatchRequestResult.created(1000L));
        String body = """
                {
                  "gameUserId": 10,
                  "teamMemberIds": [20.5, 21],
                  "comment": "함께 참가하고 싶습니다."
                }
                """;

        // when: 정상 인증으로 소수 형태의 팀원 프로필 ID를 전송한다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // then: 정수 ID로 변환하지 않고 입력 오류로 거부한다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        verifyNoInteractions(createMatchRequestUseCase, createFriendlyMatchUseCase);
    }

    // JSON 문법이 잘못된 요청은 신청 작업 전에 거부한다.
    @Test
    void JSON_문법이_잘못되면_400을_반환한다() throws Exception {
        // given: 필드 뒤에 값과 닫는 괄호가 없는 JSON이다.
        String malformedBody = "{\"gameUserId\":";

        // when: 정상 인증으로 읽을 수 없는 본문을 보낸다.
        mockMvc.perform(post("/friendly-matches/{matchId}/requests", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedBody))
                // then: JSON 파싱 오류를 공통 응답으로 반환한다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        verifyNoInteractions(createMatchRequestUseCase, createFriendlyMatchUseCase);
    }
}

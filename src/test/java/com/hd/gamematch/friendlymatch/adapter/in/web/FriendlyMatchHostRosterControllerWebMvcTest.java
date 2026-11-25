package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterLockedException;
import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchCommand;
import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchUseCase;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterCommand;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterUseCase;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FriendlyMatchController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class FriendlyMatchHostRosterControllerWebMvcTest extends SecuredWebMvcTestSupport {

    private static final String CREATE_BODY = """
            {
              "hostGameUserId": 10, "gameId": 1,
              "date": "2027-06-10", "time": "20:00:00",
              "sort": 0, "capacity": 5, "comment": "함께 경기해요"
              %s
            }
            """;
    private static final String ROSTER_BODY = """
            {"teamMemberIds": [20, 21]}
            """;
    private static final ConfirmHostRosterCommand CONFIRM_COMMAND =
            ConfirmHostRosterCommand.of(3L, 100L, List.of(20L, 21L));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateFriendlyMatchUseCase createFriendlyMatchUseCase;

    @MockitoBean
    private CreateMatchRequestUseCase createMatchRequestUseCase;

    @MockitoBean
    private ConfirmHostRosterUseCase confirmHostRosterUseCase;

    // 생성 시 선택한 호스트 팀원과 JWT 사용자를 전달하고 팀 정원을 유지한다.
    @Test
    void 호스트_팀원과_생성하면_201과_팀_정원_5를_반환한다() throws Exception {
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, 5, "함께 경기해요", List.of(20L, 21L));
        given(createFriendlyMatchUseCase.create(command)).willReturn(matchResult());

        // 팀원 두 명을 전달해도 팀당 정원은 5로 응답한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY.formatted(", \"hostTeamMemberIds\": [20, 21]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.capacity").value(5));
        then(createFriendlyMatchUseCase).should().create(command);
        verifyNoInteractions(createMatchRequestUseCase, confirmHostRosterUseCase);
    }

    // 생성 명단의 생략과 null은 미확정 입력으로 그대로 전달한다.
    @ParameterizedTest
    @ValueSource(strings = {"", ", \"hostTeamMemberIds\": null"})
    void 명단_없이_생성해도_201과_팀_정원_5를_반환한다(String rosterField) throws Exception {
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, 5, "함께 경기해요", null);
        given(createFriendlyMatchUseCase.create(command)).willReturn(matchResult());

        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY.formatted(rosterField)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.capacity").value(5));
        then(createFriendlyMatchUseCase).should().create(command);
        verifyNoInteractions(createMatchRequestUseCase, confirmHostRosterUseCase);
    }

    // 명단 확정 요청은 인증 사용자와 경로의 경기 ID를 함께 전달한다.
    @Test
    void 호스트_명단을_확정하면_경기_ID와_200을_반환한다() throws Exception {
        given(confirmHostRosterUseCase.confirm(CONFIRM_COMMAND)).willReturn(matchResult());

        mockMvc.perform(put("/friendly-matches/{matchId}/host-roster", 100L)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(ROSTER_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.capacity").value(5));
        then(confirmHostRosterUseCase).should().confirm(CONFIRM_COMMAND);
        verifyNoInteractions(createFriendlyMatchUseCase, createMatchRequestUseCase);
    }

    // 상대 승인 후 명단 변경 충돌을 공통 409 응답으로 변환한다.
    @Test
    void 참가_승인_후_명단을_변경하면_409를_반환한다() throws Exception {
        // given: 업무 계층이 첫 승인 후 명단 변경을 거부한다.
        given(confirmHostRosterUseCase.confirm(CONFIRM_COMMAND))
                .willThrow(new HostRosterLockedException());

        // when / then: 변경할 수 없는 명단이라는 오류를 응답한다.
        mockMvc.perform(put("/friendly-matches/100/host-roster")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(ROSTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FRIENDLY_MATCH_002"));
    }

    // 인증 없는 명단 확정은 업무 로직에 도달하지 않는다.
    @Test
    void JWT가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(put("/friendly-matches/100/host-roster")
                        .contentType(MediaType.APPLICATION_JSON).content(ROSTER_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(createFriendlyMatchUseCase, createMatchRequestUseCase, confirmHostRosterUseCase);
    }

    // 서비스의 호스트 권한 거부를 공통 인증 오류로 변환한다.
    @Test
    void 호스트가_아니면_403을_반환한다() throws Exception {
        given(confirmHostRosterUseCase.confirm(CONFIRM_COMMAND))
                .willThrow(new HostRosterAccessDeniedException());

        mockMvc.perform(put("/friendly-matches/100/host-roster")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(ROSTER_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
        then(confirmHostRosterUseCase).should().confirm(CONFIRM_COMMAND);
    }

    // 존재하지 않는 경기 오류는 기존 친선 경기 오류 코드를 사용한다.
    @Test
    void 경기가_없으면_404를_반환한다() throws Exception {
        given(confirmHostRosterUseCase.confirm(CONFIRM_COMMAND))
                .willThrow(new FriendlyMatchNotFoundException());

        mockMvc.perform(put("/friendly-matches/100/host-roster")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(ROSTER_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FRIENDLY_MATCH_001"));
        then(confirmHostRosterUseCase).should().confirm(CONFIRM_COMMAND);
    }

    // 경기 ID는 양의 정수만 허용한다.
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void 경기_ID가_잘못되면_400을_반환한다(String matchId) throws Exception {
        mockMvc.perform(put("/friendly-matches/{matchId}/host-roster", matchId)
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(ROSTER_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(createFriendlyMatchUseCase, createMatchRequestUseCase, confirmHostRosterUseCase);
    }

    // 확정 명단은 필수이며 각 프로필 ID는 null이 아닌 양의 정수다.
    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"teamMemberIds\": null}", "{\"teamMemberIds\": [0]}",
            "{\"teamMemberIds\": [-1]}", "{\"teamMemberIds\": [null]}",
            "{\"teamMemberIds\": [20.5]}"
    })
    void 확정_명단이_없거나_ID가_잘못되면_400을_반환한다(String body) throws Exception {
        mockMvc.perform(put("/friendly-matches/100/host-roster")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(createFriendlyMatchUseCase, createMatchRequestUseCase, confirmHostRosterUseCase);
    }

    // 선택 명단도 제공된 프로필 ID의 값과 정수 형식을 검증한다.
    @ParameterizedTest
    @ValueSource(strings = {"[0]", "[20.5]"})
    void 생성_명단의_ID가_잘못되면_400을_반환한다(String roster) throws Exception {
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY.formatted(", \"hostTeamMemberIds\": " + roster)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(createFriendlyMatchUseCase, createMatchRequestUseCase, confirmHostRosterUseCase);
    }

    // 서비스의 명단 업무 검증 실패는 공통 400 오류로 응답한다.
    @Test
    void 명단_업무_검증에_실패하면_400을_반환한다() throws Exception {
        given(confirmHostRosterUseCase.confirm(CONFIRM_COMMAND))
                .willThrow(new IllegalArgumentException("명단이 유효하지 않습니다."));

        mockMvc.perform(put("/friendly-matches/100/host-roster")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON).content(ROSTER_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        then(confirmHostRosterUseCase).should().confirm(CONFIRM_COMMAND);
    }

    // 응답 변환 검증에는 기존 경기 모델만 사용한다.
    private static FriendlyMatch matchResult() {
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        return new FriendlyMatch(
                100L, host, game, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING, 5, "함께 경기해요");
    }
}

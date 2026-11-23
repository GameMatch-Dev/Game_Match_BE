package com.hd.gamematch.friendlymatch.adapter.in.web;

import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchCommand;
import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchUseCase;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestUseCase;
import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchAccessDeniedException;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import com.hd.gamematch.global.exception.GlobalExceptionHandler;
import com.hd.gamematch.support.security.SecuredWebMvcTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FriendlyMatchController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class FriendlyMatchControllerWebMvcTest extends SecuredWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateFriendlyMatchUseCase createFriendlyMatchUseCase;

    @MockitoBean
    private CreateMatchRequestUseCase createMatchRequestUseCase;

    @Test
    void 인증된_사용자가_자신의_게임_프로필로_친선_경기_모집을_생성하면_201_응답을_반환한다() throws Exception {
        // given: Service가 생성한 매치 결과다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        FriendlyMatch friendlyMatch = new FriendlyMatch(
                100L,
                host,
                game,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                FriendlyMatchState.RECRUITING,
                5,
                "저녁에 같이 하실 분"
        );
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L,
                10L,
                1L,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                5,
                "저녁에 같이 하실 분"
        );
        given(createFriendlyMatchUseCase.create(command)).willReturn(friendlyMatch);

        // when & then: Controller는 JWT 사용자와 요청 본문을 Command로 묶어 Service에 전달한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.host.id").value(10L))
                .andExpect(jsonPath("$.data.host.nickname").value("hostA"))
                .andExpect(jsonPath("$.data.game.id").value(1L))
                .andExpect(jsonPath("$.data.capacity").value(5))
                .andExpect(jsonPath("$.data.state").value(0));
    }

    // 호스트 게임 프로필이 없으면 기존 게임 프로필 조회 예외를 404 응답으로 변환한다.
    @Test
    void 존재하지_않는_호스트_게임_프로필로_친선_경기_모집을_생성하면_404_응답을_반환한다() throws Exception {
        // given: Service가 호스트 게임 프로필을 찾지 못한 상황이다.
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L,
                999L,
                1L,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                5,
                "저녁에 같이 하실 분"
        );
        given(createFriendlyMatchUseCase.create(command))
                .willThrow(new GameUserNotFoundException());

        // when & then: 없는 호스트 프로필은 게임 프로필 없음 계약으로 응답한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 999,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GAME_USER_001"));
    }

    // 인증 사용자가 소유하지 않은 게임 프로필은 호스트로 사용할 수 없다.
    @Test
    void 타인의_게임_프로필로_친선_경기_모집을_생성하면_403_응답을_반환한다() throws Exception {
        // given: JWT 사용자 3번이 다른 사용자의 10번 게임 프로필을 호스트로 지정했다.
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L,
                10L,
                1L,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                5,
                "저녁에 같이 하실 분"
        );
        given(createFriendlyMatchUseCase.create(command))
                .willThrow(new FriendlyMatchAccessDeniedException());

        // when & then: 소유권 위반은 저장하지 않고 권한 오류로 응답한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_403"))
                .andExpect(jsonPath("$.message").value("친선 경기 모집을 만들 권한이 없습니다."));
    }

    // 요청 게임과 호스트 게임 프로필의 게임이 다르면 다른 게임의 매치를 만들 수 없다.
    @Test
    void 호스트_게임_프로필과_다른_게임으로_친선_경기_모집을_생성하면_400_응답을_반환한다() throws Exception {
        // given: 10번 호스트 프로필은 1번 게임용인데 요청은 2번 게임을 지정했다.
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L,
                10L,
                2L,
                LocalDate.of(2027, 6, 10),
                LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM,
                5,
                "저녁에 같이 하실 분"
        );
        given(createFriendlyMatchUseCase.create(command))
                .willThrow(new IllegalArgumentException("호스트 게임 프로필의 게임과 요청 게임이 일치하지 않습니다."));

        // when & then: 게임 불일치는 저장하지 않고 공통 400 오류로 응답한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 2,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    // sort는 친선 경기 방식 enum 범위 안의 값만 받을 수 있다.
    @Test
    void 범위를_벗어난_sort로_친선_경기_모집을_생성하면_400_응답을_반환한다() throws Exception {
        // when & then: TEAM(0), SOLO(1)가 아닌 값은 Service 호출 전에 요청 오류로 처리한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 2,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    // 참가 정원은 한 명 이상이어야 한다.
    @Test
    void 참가_정원이_0이면_친선_경기_모집을_생성할_수_없다() throws Exception {
        // when & then: 0명 정원 요청은 Service 호출 전에 요청 오류로 처리한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 0,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    // 호스트 프로필이 없는 요청은 Service를 호출하기 전에 거부한다.
    @Test
    void 호스트_게임_프로필_ID가_없으면_400_응답을_반환한다() throws Exception {
        // when: 인증은 정상이지만 hostGameUserId를 생략한 요청을 보낸다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                // then: 입력 검증 실패를 공통 400 응답으로 반환한다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // 잘못된 입력으로 생성 작업을 시작하지 않았는지도 확인한다.
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 0과 음수 ID는 프로필 조회를 시작하기 전에 입력 오류로 거부한다.
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void 호스트_게임_프로필_ID가_양수가_아니면_400_응답을_반환한다(Long hostGameUserId) throws Exception {
        // when: 다른 값은 정상으로 두고 호스트 ID만 0 또는 음수로 보낸다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %d,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """.formatted(hostGameUserId)))
                // then: 잘못된 ID는 공통 400 응답으로 반환한다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // 입력 검증에서 거부했으므로 생성 UseCase를 호출하지 않는다.
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 게임 ID 누락, 0, 음수는 입력 검증에서 거부한다.
    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void 게임_ID가_없거나_양수가_아니면_400_응답을_반환한다(Long gameId) throws Exception {
        // null 케이스는 JSON에서 gameId 자체를 생략해 누락 요청을 검증한다.
        String gameIdField = gameId == null ? "" : "\"gameId\": " + gameId + ",";

        // when: 호스트 등 나머지 입력은 정상으로 두고 게임 ID만 바꾼다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  %s
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """.formatted(gameIdField)))
                // then: 잘못된 게임 ID는 공통 400 응답으로 반환한다.
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // 입력 검증 실패이므로 게임 확인이나 매치 생성 작업을 시작하지 않는다.
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 경기 날짜는 생성 요청의 필수값이다.
    @Test
    void 경기_날짜가_없으면_400_응답을_반환한다() throws Exception {
        // when: date만 생략하고 나머지 필드는 정상값으로 보낸다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // then: 필수값 검증에서 거부되어 생성 로직을 실행하지 않는다.
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 경기 시간는 생성 요청의 필수값이다.
    @Test
    void 경기_시간이_없으면_400_응답을_반환한다() throws Exception {
        // when: time만 생략하고 나머지 필드는 정상값으로 보낸다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "sort": 0,
                                  "capacity": 5,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // then: 필수값 검증에서 거부되어 생성 로직을 실행하지 않는다.
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 소개 누락, 빈 문자열, 공백, 최대 길이 초과를 입력 오류로 거부한다.
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 1, 501})
    void 소개가_없거나_공백이거나_500자를_넘으면_400을_반환한다(Integer length) throws Exception {
        // null 입력은 comment 필드를 생략하고, 501 입력은 길이 초과 문자열을 보낸다.
        String commentField = length == null ? "" : ", \"comment\": \"" + (length > 1 ? "가" : " ").repeat(length) + "\"";
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10, "gameId": 1,
                                  "date": "2027-06-10", "time": "20:00:00",
                                  "sort": 0, "capacity": 5
                                  %s
                                }
                                """.formatted(commentField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // 유효하지 않은 소개는 생성 로직에 전달하지 않는다.
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 최대 길이 경계인 500자는 정상 요청으로 처리한다.
    @Test
    void 소개가_500자이면_생성할_수_있다() throws Exception {
        String comment = "가".repeat(500);
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        CreateFriendlyMatchCommand command = CreateFriendlyMatchCommand.of(
                3L, 10L, 1L, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, 5, comment);
        FriendlyMatch result = new FriendlyMatch(
                100L, host, game, command.date(), command.time(), command.sort(),
                FriendlyMatchState.RECRUITING, 5, comment);
        given(createFriendlyMatchUseCase.create(command)).willReturn(result);

        // 최대 허용 길이가 잘못 거부되거나 응답에서 잘리지 않는지 확인한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10, "gameId": 1,
                                  "date": "2027-06-10", "time": "20:00:00",
                                  "sort": 0, "capacity": 5, "comment": "%s"
                                }
                                """.formatted(comment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comment").value(comment));
        then(createFriendlyMatchUseCase).should().create(command);
    }

    // 날짜와 시간의 파싱 오류는 생성 로직을 실행하지 않고 400으로 응답한다.
    @ParameterizedTest
    @CsvSource({
            "2027-13-10, 20:00:00",
            "2027-06-10, 25:00:00"
    })
    void 날짜나_시간_형식이_잘못되면_400을_반환한다(String date, String time) throws Exception {
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10, "gameId": 1,
                                  "date": "%s", "time": "%s",
                                  "sort": 0, "capacity": 5, "comment": "정상 소개"
                                }
                                """.formatted(date, time)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 정수 필드에 소수를 보내면 잘라서 사용하지 않고 입력 오류로 거부한다.
    @ParameterizedTest
    @CsvSource({
            "10, 1, 0, 1.5",
            "10, 1, 0.5, 5",
            "10.5, 1, 0, 5",
            "10, 1.5, 0, 5"
    })
    void 정수_필드에_소수를_보내면_400을_반환한다(
            String hostId, String gameId, String sort, String capacity
    ) throws Exception {
        // 파싱이 잘못 통과해도 NPE로 숨겨지지 않도록 정상 생성 결과를 준비한다.
        Game game = Game.of(1L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile host = new GameUserProfile(10L, "hostA", game, 3L);
        FriendlyMatch result = new FriendlyMatch(
                100L, host, game, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING, 5, "정상 소개");
        given(createFriendlyMatchUseCase.create(org.mockito.ArgumentMatchers.any())).willReturn(result);

        // 각 요청에서는 정수 필드 한 개에만 소수를 넣는다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": %s, "gameId": %s,
                                  "date": "2027-06-10", "time": "20:00:00",
                                  "sort": %s, "capacity": %s, "comment": "정상 소개"
                                }
                                """.formatted(hostId, gameId, sort, capacity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        // 유효하지 않은 숫자가 생성 로직에 전달되지 않아야 한다.
        then(createFriendlyMatchUseCase).shouldHaveNoInteractions();
    }

    // 참가 정원은 친선 경기 생성에 필요한 필수값이다.
    @Test
    void 참가_정원이_없으면_친선_경기_모집을_생성할_수_없다() throws Exception {
        // when & then: capacity가 없는 요청은 Service 호출 전에 요청 오류로 처리한다.
        mockMvc.perform(post("/friendly-matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostGameUserId": 10,
                                  "gameId": 1,
                                  "date": "2027-06-10",
                                  "time": "20:00:00",
                                  "sort": 0,
                                  "comment": "저녁에 같이 하실 분"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }
}

package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchNotRecruitingException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAlreadyActiveException;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.request.CreateMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestPort;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestTargetPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveMatchRequestPort;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.friendlymatch.domain.MatchRequestTarget;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CreateMatchRequestServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long MATCH_ID = 100L;
    private static final Long GAME_USER_ID = 10L;
    private static final Long REQUEST_ID = 1000L;
    private static final String COMMENT = "함께 경기하고 싶습니다.";
    private static final Game GAME = Game.of(2L, "League of Legends", "MOBA", null);
    private static final Game OTHER_GAME = Game.of(3L, "Overwatch", "FPS", null);
    private static final GameUserProfile REQUESTER = new GameUserProfile(
            GAME_USER_ID, "requester", GAME, USER_ID);

    @Mock
    private FindGameUserUseCase findGameUserUseCase;
    @Mock
    private LoadMatchRequestTargetPort loadMatchRequestTargetPort;
    @Mock
    private LoadMatchRequestPort loadMatchRequestPort;
    @Mock
    private SaveMatchRequestPort saveMatchRequestPort;
    @InjectMocks
    private CreateMatchRequestService service;

    // 개인 신청은 팀원 없이 대기 상태로 새로 저장한다.
    @Test
    void 개인_신청을_새로_생성한다() {
        // given
        givenRequester();
        givenRecruitingTarget();
        givenNoExistingRequest();
        given(saveMatchRequestPort.save(any(MatchRequest.class))).willReturn(REQUEST_ID);

        // when
        CreateMatchRequestResult result = service.createRequest(command(List.of()));

        // then
        assertNewRequest(captureSavedRequest(), List.of());
        assertThat(result.requestId()).isEqualTo(REQUEST_ID);
        assertThat(result.created()).isTrue();
    }

    // 팀원 목록을 생략하면 빈 목록인 개인 신청으로 저장한다.
    @Test
    void 팀원_목록이_null이면_개인_신청으로_생성한다() {
        // given
        givenRequester();
        givenRecruitingTarget();
        givenNoExistingRequest();
        given(saveMatchRequestPort.save(any(MatchRequest.class))).willReturn(REQUEST_ID);

        // when
        CreateMatchRequestResult result = service.createRequest(command(null));

        // then
        assertNewRequest(captureSavedRequest(), List.of());
        assertThat(result.requestId()).isEqualTo(REQUEST_ID);
        assertThat(result.created()).isTrue();
    }

    // 팀원은 신청자와 소유자가 달라도 같은 게임이면 신청할 수 있다.
    @Test
    void 다른_사용자의_프로필을_팀원으로_포함하여_신청한다() {
        // given
        List<Long> teamMemberIds = List.of(20L, 21L);
        givenRequester();
        givenRecruitingTarget();
        given(findGameUserUseCase.findGameUsers(teamMemberIds))
                .willReturn(List.of(new GameUserProfile(20L, "teammateA", GAME, 5L),
                        new GameUserProfile(21L, "teammateB", GAME, 6L)));
        givenNoExistingRequest();
        given(saveMatchRequestPort.save(any(MatchRequest.class))).willReturn(REQUEST_ID);

        // when
        CreateMatchRequestResult result = service.createRequest(command(teamMemberIds));

        // then
        assertNewRequest(captureSavedRequest(), teamMemberIds);
        assertThat(result.requestId()).isEqualTo(REQUEST_ID);
        assertThat(result.created()).isTrue();
        verify(findGameUserUseCase).findGameUsers(teamMemberIds);
        verify(findGameUserUseCase, never()).findGameUser(FindGameUserQuery.of(20L));
        verify(findGameUserUseCase, never()).findGameUser(FindGameUserQuery.of(21L));
    }

    // 인증 사용자와 신청 프로필 소유자가 다르면 저장하지 않는다.
    @Test
    void 다른_사용자의_프로필로_신청하면_접근을_거부한다() {
        // given
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(GAME_USER_ID)))
                .willReturn(new GameUserProfile(GAME_USER_ID, "other", GAME, 9L));

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of())))
                .isInstanceOf(MatchRequestAccessDeniedException.class);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 신청 프로필 조회 오류는 원래 예외를 전달한다.
    @Test
    void 신청_프로필이_없으면_조회_예외를_전달한다() {
        // given
        GameUserNotFoundException failure = new GameUserNotFoundException();
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(GAME_USER_ID)))
                .willThrow(failure);

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of()))).isSameAs(failure);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 대상 경기가 없으면 신청을 저장하지 않는다.
    @Test
    void 대상_경기가_없으면_예외를_던진다() {
        // given
        givenRequester();
        given(loadMatchRequestTargetPort.loadMatchRequestTarget(MATCH_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of())))
                .isInstanceOf(FriendlyMatchNotFoundException.class);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 모집이 끝난 경기에는 신청할 수 없다.
    @Test
    void 모집_중이_아니면_예외를_던진다() {
        // given
        givenRequester();
        given(loadMatchRequestTargetPort.loadMatchRequestTarget(MATCH_ID))
                .willReturn(Optional.of(new MatchRequestTarget(MATCH_ID, GAME.id(), false)));

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of())))
                .isInstanceOf(MatchNotRecruitingException.class);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 신청 프로필은 대상 경기와 같은 게임이어야 한다.
    @Test
    void 신청_프로필의_게임이_다르면_예외를_던진다() {
        // given
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(GAME_USER_ID)))
                .willReturn(new GameUserProfile(GAME_USER_ID, "requester", OTHER_GAME, USER_ID));
        givenRecruitingTarget();

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 모든 팀원 프로필도 대상 경기와 같은 게임이어야 한다.
    @Test
    void 팀원의_게임이_다르면_예외를_던진다() {
        // given
        givenRequester();
        givenRecruitingTarget();
        given(findGameUserUseCase.findGameUsers(List.of(20L)))
                .willReturn(List.of(new GameUserProfile(20L, "teammate", OTHER_GAME, 5L)));

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of(20L))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 팀원 프로필 조회 오류도 원래 예외를 전달한다.
    @Test
    void 팀원_프로필이_없으면_조회_예외를_전달한다() {
        // given
        givenRequester();
        givenRecruitingTarget();
        GameUserNotFoundException failure = new GameUserNotFoundException();
        given(findGameUserUseCase.findGameUsers(List.of(20L))).willThrow(failure);

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of(20L)))).isSameAs(failure);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 팀원 목록 안의 중복과 신청자 프로필의 재포함을 거부한다.
    @ParameterizedTest
    @MethodSource("duplicateRosters")
    void 참가_프로필이_중복되면_예외를_던진다(List<Long> teamMemberIds) {
        // given
        // 중복 검증과 조회의 순서는 계약에 없으므로 이 두 조회만 선택적으로 허용한다.
        lenient().when(findGameUserUseCase.findGameUser(FindGameUserQuery.of(GAME_USER_ID)))
                .thenReturn(REQUESTER);
        lenient().when(loadMatchRequestTargetPort.loadMatchRequestTarget(MATCH_ID))
                .thenReturn(Optional.of(new MatchRequestTarget(MATCH_ID, GAME.id(), true)));

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(teamMemberIds)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 대기 또는 승인 상태의 기존 신청은 다시 활성화할 수 없다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"PENDING", "APPROVED"})
    void 활성_신청이_있으면_예외를_던진다(MatchRequestState state) {
        // given
        givenRequester();
        givenRecruitingTarget();
        given(loadMatchRequestPort.loadByMatchIdAndGameUserId(MATCH_ID, GAME_USER_ID))
                .willReturn(Optional.of(new MatchRequest(
                        REQUEST_ID, MATCH_ID, GAME_USER_ID, List.of(), COMMENT, state)));

        // when / then
        assertThatThrownBy(() -> service.createRequest(command(List.of())))
                .isInstanceOf(MatchRequestAlreadyActiveException.class);
        verifyNoInteractions(saveMatchRequestPort);
    }

    // 취소 또는 거절된 신청은 새 명단과 메시지로 갱신하여 대기 상태가 된다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"CANCELLED", "REJECTED"})
    void 비활성_신청을_기존_ID로_재활성화한다(MatchRequestState state) {
        // given
        List<Long> teamMemberIds = List.of(20L, 21L);
        givenRequester();
        givenRecruitingTarget();
        given(findGameUserUseCase.findGameUsers(teamMemberIds))
                .willReturn(List.of(new GameUserProfile(20L, "teammateA", GAME, 5L),
                        new GameUserProfile(21L, "teammateB", GAME, 6L)));
        given(loadMatchRequestPort.loadByMatchIdAndGameUserId(MATCH_ID, GAME_USER_ID))
                .willReturn(Optional.of(new MatchRequest(
                        REQUEST_ID, MATCH_ID, GAME_USER_ID, List.of(30L), "기존 메시지", state)));
        given(saveMatchRequestPort.save(any(MatchRequest.class))).willReturn(REQUEST_ID);

        // when
        CreateMatchRequestResult result = service.createRequest(command(teamMemberIds));

        // then
        MatchRequest saved = captureSavedRequest();
        assertThat(saved.id()).isEqualTo(REQUEST_ID);
        assertThat(saved.state()).isEqualTo(MatchRequestState.PENDING);
        assertThat(saved.teamMemberIds()).containsExactlyElementsOf(teamMemberIds);
        assertThat(saved.comment()).isEqualTo(COMMENT);
        assertThat(result.requestId()).isEqualTo(REQUEST_ID);
        assertThat(result.created()).isFalse();
    }

    // 팀원 중복과 신청자 재포함 사례를 제공한다.
    private static Stream<List<Long>> duplicateRosters() {
        return Stream.of(List.of(20L, 20L), List.of(GAME_USER_ID));
    }

    // 팀원 목록을 포함한 신청 명령을 만든다.
    private CreateMatchRequestCommand command(List<Long> teamMemberIds) {
        return CreateMatchRequestCommand.of(USER_ID, MATCH_ID, GAME_USER_ID, teamMemberIds, COMMENT);
    }

    // 인증 사용자가 소유한 신청 프로필을 준비한다.
    private void givenRequester() {
        given(findGameUserUseCase.findGameUser(FindGameUserQuery.of(GAME_USER_ID))).willReturn(REQUESTER);
    }

    // 같은 게임의 모집 중인 대상 경기를 준비한다.
    private void givenRecruitingTarget() {
        given(loadMatchRequestTargetPort.loadMatchRequestTarget(MATCH_ID))
                .willReturn(Optional.of(new MatchRequestTarget(MATCH_ID, GAME.id(), true)));
    }

    // 기존 참가 신청이 없는 조회 결과를 준비한다.
    private void givenNoExistingRequest() {
        given(loadMatchRequestPort.loadByMatchIdAndGameUserId(MATCH_ID, GAME_USER_ID))
                .willReturn(Optional.empty());
    }

    // 저장 포트에 전달된 신청을 캡처한다.
    private MatchRequest captureSavedRequest() {
        ArgumentCaptor<MatchRequest> captor = ArgumentCaptor.forClass(MatchRequest.class);
        verify(saveMatchRequestPort).save(captor.capture());
        return captor.getValue();
    }

    // 신규 신청의 입력값과 대기 상태를 검증한다.
    private void assertNewRequest(MatchRequest saved, List<Long> teamMemberIds) {
        assertThat(saved.id()).isNull();
        assertThat(saved.matchId()).isEqualTo(MATCH_ID);
        assertThat(saved.gameUserId()).isEqualTo(GAME_USER_ID);
        assertThat(saved.teamMemberIds()).containsExactlyElementsOf(teamMemberIds);
        assertThat(saved.comment()).isEqualTo(COMMENT);
        assertThat(saved.state()).isEqualTo(MatchRequestState.PENDING);
    }
}

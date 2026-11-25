package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchApprovalAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.exception.MatchApprovalConflictException;
import com.hd.gamematch.friendlymatch.application.exception.MatchNotRecruitingException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestNotFoundException;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestCommand;
import com.hd.gamematch.friendlymatch.application.port.in.approve.ApproveMatchRequestResult;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchApprovalContextPort;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestForApprovalPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveMatchRequestApprovalPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.friendlymatch.domain.MatchApprovalContext;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ApproveMatchRequestServiceTest {

    private static final Long HOST_USER_ID = 3L;
    private static final Long MATCH_ID = 100L;
    private static final Long REQUEST_ID = 1L;
    private static final Game GAME = Game.of(1L, "League of Legends", "MOBA", null);
    private static final List<Long> REQUEST_PARTICIPANTS = List.of(20L, 21L);
    private static final ApproveMatchRequestCommand COMMAND =
            ApproveMatchRequestCommand.of(HOST_USER_ID, REQUEST_ID);

    @Mock
    private LoadMatchRequestForApprovalPort loadMatchRequestForApprovalPort;
    @Mock
    private LoadMatchApprovalContextPort loadMatchApprovalContextPort;
    @Mock
    private SaveMatchRequestApprovalPort saveMatchRequestApprovalPort;
    @Mock
    private FindGameUserUseCase findGameUserUseCase;
    @InjectMocks
    private ApproveMatchRequestService service;

    // 이 테스트는 잠금 포트 호출 순서를 검증하며 실제 DB 잠금과 롤백은 통합 테스트에서 검증한다.
    @Test
    void 경기_잠금_후_신청을_다시_읽고_팀전의_부분_명단을_승인한다() {
        // given: 주최 측 1명과 상대 2명은 팀별 정원 5 이하이며 같은 인원일 필요가 없다.
        prepare(context(FriendlyMatchSort.TEAM, 5, true, List.of(10L), List.of(), false),
                request(MatchRequestState.PENDING));
        given(findGameUserUseCase.findGameUsers(REQUEST_PARTICIPANTS))
                .willReturn(profiles(REQUEST_PARTICIPANTS));

        // when: 경기 주최자가 대기 중인 상대 신청을 승인한다.
        ApproveMatchRequestResult result = service.approve(COMMAND);

        // then: 대표를 포함한 상대 전체를 등록하고 TEAM 모집을 닫는 저장 요청을 전달한다.
        assertThat(result).isEqualTo(expectedResult());
        InOrder order = inOrder(loadMatchRequestForApprovalPort, loadMatchApprovalContextPort,
                findGameUserUseCase, saveMatchRequestApprovalPort);
        order.verify(loadMatchRequestForApprovalPort).findMatchIdByRequestId(REQUEST_ID);
        order.verify(loadMatchApprovalContextPort).loadForUpdate(MATCH_ID);
        order.verify(loadMatchRequestForApprovalPort).loadRequestById(REQUEST_ID);
        order.verify(findGameUserUseCase).findGameUsers(REQUEST_PARTICIPANTS);
        order.verify(saveMatchRequestApprovalPort)
                .saveApproval(MATCH_ID, REQUEST_ID, REQUEST_PARTICIPANTS, true);
        then(saveMatchRequestApprovalPort).shouldHaveNoMoreInteractions();
    }

    // TEAM 정원은 두 팀 합계가 아닌 각 팀의 최대 인원이다.
    @Test
    void 양_팀이_각각_정원과_같아도_팀전_승인을_허용한다() {
        // given: 팀별 정원 2에 주최 2명과 상대 2명을 준비한다.
        prepare(context(FriendlyMatchSort.TEAM, 2, true, List.of(10L, 11L), List.of(), false),
                request(MatchRequestState.PENDING));
        given(findGameUserUseCase.findGameUsers(REQUEST_PARTICIPANTS))
                .willReturn(profiles(REQUEST_PARTICIPANTS));

        // when / then: 전체 4명을 capacity=2와 비교하지 않고 승인한다.
        assertThat(service.approve(COMMAND)).isEqualTo(expectedResult());
        then(saveMatchRequestApprovalPort).should()
                .saveApproval(MATCH_ID, REQUEST_ID, REQUEST_PARTICIPANTS, true);
    }

    // 명시적으로 확정한 0명 명단도 승인 조건을 만족하며 주최자를 자동 합산하지 않는다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 빈_주최_명단이_확정되어_있으면_승인한다(FriendlyMatchSort sort) {
        // given: 상대 2명만으로 정원 2를 채우는 경기를 준비한다.
        prepare(context(sort, 2, true, List.of(), List.of(), false), request(MatchRequestState.PENDING));
        given(findGameUserUseCase.findGameUsers(REQUEST_PARTICIPANTS))
                .willReturn(profiles(REQUEST_PARTICIPANTS));

        // when / then: 비어 있음과 미확정을 구분하고 숨은 주최자 1명을 더하지 않는다.
        assertThat(service.approve(COMMAND)).isEqualTo(expectedResult());
        then(saveMatchRequestApprovalPort).should()
                .saveApproval(MATCH_ID, REQUEST_ID, REQUEST_PARTICIPANTS, true);
    }

    // 개인전은 주최 명단과 기존 승인 명단까지 합산해 정원 도달 여부를 판단한다.
    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void 개인전은_전체_인원이_정원에_도달할_때만_모집을_닫는다(int capacity) {
        // given: 주최 1명, 기존 승인 1명, 새 신청 2명으로 전체 4명을 준비한다.
        prepare(context(FriendlyMatchSort.INDIVIDUAL, capacity, true,
                List.of(10L), List.of(30L), false), request(MatchRequestState.PENDING));
        given(findGameUserUseCase.findGameUsers(REQUEST_PARTICIPANTS))
                .willReturn(profiles(REQUEST_PARTICIPANTS));

        // when / then: 정원이 4면 닫고 5면 모집 상태를 유지한다.
        assertThat(service.approve(COMMAND)).isEqualTo(expectedResult());
        then(saveMatchRequestApprovalPort).should()
                .saveApproval(MATCH_ID, REQUEST_ID, REQUEST_PARTICIPANTS, capacity == 4);
    }

    // 처음 신청을 찾지 못하면 경기 잠금과 저장을 시도하지 않는다.
    @Test
    void 신청이_없으면_신청_미존재_예외를_던진다() {
        // given: 신청 ID에 대응하는 경기 ID가 없다.
        given(loadMatchRequestForApprovalPort.findMatchIdByRequestId(REQUEST_ID)).willReturn(Optional.empty());

        // when / then: 조회 단계에서 종료하고 후속 작업을 수행하지 않는다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchRequestNotFoundException.class);
        verifyNoInteractions(loadMatchApprovalContextPort, findGameUserUseCase, saveMatchRequestApprovalPort);
        then(loadMatchRequestForApprovalPort).should().findMatchIdByRequestId(REQUEST_ID);
        then(loadMatchRequestForApprovalPort).shouldHaveNoMoreInteractions();
    }

    // 경기 행이 없으면 승인 문맥을 만들지 못하므로 신청을 변경하지 않는다.
    @Test
    void 경기가_없으면_경기_미존재_예외를_던진다() {
        // given: 신청에는 경기 ID가 있지만 경기 조회 결과가 없다.
        given(loadMatchRequestForApprovalPort.findMatchIdByRequestId(REQUEST_ID))
                .willReturn(Optional.of(MATCH_ID));
        given(loadMatchApprovalContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.empty());

        // when / then: 참가자 조회와 저장 전에 경기 미존재로 종료한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(FriendlyMatchNotFoundException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
        then(loadMatchRequestForApprovalPort).should().findMatchIdByRequestId(REQUEST_ID);
        then(loadMatchRequestForApprovalPort).shouldHaveNoMoreInteractions();
    }

    // 잠금을 기다린 뒤 신청이 사라졌다면 최초 조회 결과만으로 처리하지 않는다.
    @Test
    void 잠금_후_신청을_찾지_못하면_저장하지_않는다() {
        // given: 경기 ID를 찾은 뒤 최신 신청 조회는 비어 있다.
        given(loadMatchRequestForApprovalPort.findMatchIdByRequestId(REQUEST_ID))
                .willReturn(Optional.of(MATCH_ID));
        given(loadMatchApprovalContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(teamContext()));
        given(loadMatchRequestForApprovalPort.loadRequestById(REQUEST_ID)).willReturn(Optional.empty());

        // when / then: 최신 신청이 없으면 미존재 예외로 종료한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchRequestNotFoundException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 재승인도 주최자에게만 허용하며 다른 사용자는 참가자를 조회하거나 수정할 수 없다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"PENDING", "APPROVED"})
    void 주최자가_아니면_현재_신청_상태와_무관하게_거부한다(MatchRequestState state) {
        // given: 사용자 3이 주최한 경기의 신청을 준비한다.
        prepare(teamContext(), request(state));

        // when / then: 사용자 4의 요청은 재호출 성공 처리보다 먼저 권한으로 거부한다.
        assertThatThrownBy(() -> service.approve(ApproveMatchRequestCommand.of(4L, REQUEST_ID)))
                .isInstanceOf(MatchApprovalAccessDeniedException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 승인 완료 요청은 모집 종료 후 재호출해도 성공하며 참가자를 중복 등록하지 않는다.
    @Test
    void 이미_승인된_신청은_모집이_닫혀도_저장_없이_같은_결과를_반환한다() {
        // given: 해당 신청의 두 참가자가 이미 등록되어 있고 TEAM 모집도 종료됐다.
        prepare(new MatchApprovalContext(MATCH_ID, HOST_USER_ID, GAME.id(), FriendlyMatchSort.TEAM,
                FriendlyMatchState.RECRUITMENT_CLOSED, 5, true, List.of(10L),
                REQUEST_PARTICIPANTS, true), request(MatchRequestState.APPROVED));

        // when / then: 중복 검사와 모집 상태 검사에 걸리지 않고 읽기 결과만 반환한다.
        assertThat(service.approve(COMMAND)).isEqualTo(expectedResult());
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 취소와 거절 상태를 직접 승인으로 되돌리는 것은 허용하지 않는다.
    @ParameterizedTest
    @EnumSource(value = MatchRequestState.class, names = {"CANCELLED", "REJECTED"})
    void 취소되거나_거절된_신청은_승인하지_않는다(MatchRequestState state) {
        // given: 명단과 정원은 유효하지만 승인 가능한 대기 상태가 아니다.
        prepare(teamContext(), request(state));

        // when / then: 상태 충돌을 반환하고 참가자 등록을 수행하지 않는다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchApprovalConflictException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 명단 내용이 비어 있지 않더라도 미확정이면 상대 승인을 막는다.
    @Test
    void 주최_명단이_미확정이면_승인하지_않는다() {
        // given: 명단은 있지만 확정 여부는 false다.
        prepare(context(FriendlyMatchSort.TEAM, 5, false, List.of(10L), List.of(), false),
                request(MatchRequestState.PENDING));

        // when / then: 명단 확정 여부를 검사하고 저장 전에 종료한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchApprovalConflictException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 모집 종료 후 다른 대기 신청을 새로 승인하지 않는다.
    @Test
    void 모집이_종료된_경기의_대기_신청은_승인하지_않는다() {
        // given: 상대 팀 승인 여부와 별개로 모집이 닫힌 경기를 준비한다.
        prepare(new MatchApprovalContext(MATCH_ID, HOST_USER_ID, GAME.id(), FriendlyMatchSort.INDIVIDUAL,
                FriendlyMatchState.RECRUITMENT_CLOSED, 5, true, List.of(10L), List.of(), false),
                request(MatchRequestState.PENDING));

        // when / then: 모집 중이 아니라는 기존 업무 예외를 반환한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchNotRecruitingException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // TEAM은 정원에 여유가 있어도 상대 신청을 두 건 승인하지 않는다.
    @Test
    void 팀전의_상대_신청이_이미_승인되었으면_다른_팀을_승인하지_않는다() {
        // given: 모집 상태가 열려 있더라도 다른 상대 팀이 승인된 상태다.
        prepare(context(FriendlyMatchSort.TEAM, 5, true, List.of(10L), List.of(30L), true),
                request(MatchRequestState.PENDING));

        // when / then: 다른 상대 팀 존재 자체를 승인 충돌로 처리한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchApprovalConflictException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 두 모집 방식의 정원 산식에 따라 초과 승인을 저장 전에 거부한다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 승인하면_정원을_초과하는_신청은_등록하지_않는다(FriendlyMatchSort sort) {
        // given: TEAM은 상대 2명이 정원 1을 넘고 개인전은 전체 3명이 정원 2를 넘는다.
        int capacity = sort == FriendlyMatchSort.TEAM ? 1 : 2;
        prepare(context(sort, capacity, true, List.of(10L), List.of(), false),
                request(MatchRequestState.PENDING));

        // when / then: 초과한 승인 전체를 거부하고 저장을 호출하지 않는다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchApprovalConflictException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 주최 측 실제 참가자가 상대 신청에도 있으면 양측 중복 참가를 막는다.
    @Test
    void 주최_명단과_신청_명단이_겹치면_승인하지_않는다() {
        // given: 주최 측 명단에 이미 상대 팀원 21번이 포함되어 있다.
        prepare(context(FriendlyMatchSort.TEAM, 5, true, List.of(21L), List.of(), false),
                request(MatchRequestState.PENDING));

        // when / then: 어느 참가자도 저장하지 않고 승인 전체를 거부한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchApprovalConflictException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 개인전에서 다른 신청으로 먼저 승인된 참가자를 다시 등록하지 않는다.
    @Test
    void 기존_승인_참가자와_신청_명단이_겹치면_승인하지_않는다() {
        // given: 새 신청의 대표 20번이 다른 신청으로 이미 참가 중이다.
        prepare(context(FriendlyMatchSort.INDIVIDUAL, 5, true, List.of(10L), List.of(20L), false),
                request(MatchRequestState.PENDING));

        // when / then: 남은 정원이 있어도 중복 참가 충돌로 거부한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchApprovalConflictException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 저장된 신청 명단도 승인 시 대표를 포함해 중복을 검사한다.
    @Test
    void 신청_대표가_팀원에도_중복되어_있으면_승인하지_않는다() {
        // given: 대표 20번을 추가 팀원에도 포함한 신청을 준비한다.
        prepare(teamContext(), new MatchRequest(REQUEST_ID, MATCH_ID, 20L,
                List.of(20L), "함께 참가합니다", MatchRequestState.PENDING));

        // when / then: 중복 참가자를 제거해 몰래 승인하지 않고 입력 전체를 거부한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(MatchApprovalConflictException.class);
        verifyNoInteractions(findGameUserUseCase, saveMatchRequestApprovalPort);
    }

    // 승인을 기다리는 동안 프로필이 사라진 경우 참가자 등록을 막는다.
    @Test
    void 신청_명단의_프로필을_찾지_못하면_승인하지_않는다() {
        // given: 대표와 팀원을 한 번에 조회하는 과정에서 미존재 예외가 발생한다.
        prepare(teamContext(), request(MatchRequestState.PENDING));
        given(findGameUserUseCase.findGameUsers(REQUEST_PARTICIPANTS))
                .willThrow(new GameUserNotFoundException());

        // when / then: 기존 프로필 미존재 예외를 유지하고 승인 저장을 수행하지 않는다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(GameUserNotFoundException.class);
        verifyNoInteractions(saveMatchRequestApprovalPort);
    }

    // 신청 명단의 프로필은 승인 대상 경기와 같은 게임이어야 한다.
    @Test
    void 신청_명단에_다른_게임_프로필이_있으면_승인하지_않는다() {
        // given: 팀원 21번만 다른 게임의 프로필로 조회된다.
        prepare(teamContext(), request(MatchRequestState.PENDING));
        Game otherGame = Game.of(2L, "Valorant", "FPS", null);
        given(findGameUserUseCase.findGameUsers(REQUEST_PARTICIPANTS)).willReturn(List.of(
                new GameUserProfile(20L, "player20", GAME, 120L),
                new GameUserProfile(21L, "player21", otherGame, 121L)));

        // when / then: 다른 게임 참가자를 포함한 승인 전체를 입력 오류로 거부한다.
        assertThatThrownBy(() -> service.approve(COMMAND)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveMatchRequestApprovalPort);
    }

    // 신청이 가리키는 경기부터 찾고 잠금 포트 이후의 최신 신청을 준비한다.
    private void prepare(MatchApprovalContext context, MatchRequest request) {
        given(loadMatchRequestForApprovalPort.findMatchIdByRequestId(REQUEST_ID))
                .willReturn(Optional.of(MATCH_ID));
        given(loadMatchApprovalContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(context));
        given(loadMatchRequestForApprovalPort.loadRequestById(REQUEST_ID)).willReturn(Optional.of(request));
    }

    // 기본 TEAM 승인을 위한 확정된 주최 명단과 비어 있는 상대 명단을 만든다.
    private MatchApprovalContext teamContext() {
        return context(FriendlyMatchSort.TEAM, 5, true, List.of(10L), List.of(), false);
    }

    // 경기 잠금 후 승인 판단에 필요한 현재 명단과 상태를 전달할 조회 결과다.
    private MatchApprovalContext context(FriendlyMatchSort sort, int capacity, boolean confirmed,
                                         List<Long> hostIds, List<Long> approvedIds, boolean opponentApproved) {
        return new MatchApprovalContext(MATCH_ID, HOST_USER_ID, GAME.id(), sort,
                FriendlyMatchState.RECRUITING, capacity, confirmed, hostIds, approvedIds, opponentApproved);
    }

    // 신청 대표는 20번이며 추가 팀원 21번과 함께 실제 참가자 두 명을 구성한다.
    private MatchRequest request(MatchRequestState state) {
        return new MatchRequest(REQUEST_ID, MATCH_ID, 20L, List.of(21L), "함께 참가합니다", state);
    }

    // 새로운 승인 또는 동일 신청 재승인에서 반환할 공개 결과다.
    private ApproveMatchRequestResult expectedResult() {
        return ApproveMatchRequestResult.of(REQUEST_ID, MATCH_ID, MatchRequestState.APPROVED,
                REQUEST_PARTICIPANTS);
    }

    // 일괄 조회 응답용으로 같은 게임의 실제 참가자 프로필을 만든다.
    private List<GameUserProfile> profiles(List<Long> ids) {
        return ids.stream().map(id -> new GameUserProfile(id, "player" + id, GAME, 100L + id)).toList();
    }
}

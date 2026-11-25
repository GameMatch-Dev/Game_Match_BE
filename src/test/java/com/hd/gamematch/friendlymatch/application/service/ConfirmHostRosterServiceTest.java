package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterCommand;
import com.hd.gamematch.friendlymatch.application.port.out.LoadHostRosterEditContextPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveHostRosterPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.HostRosterEditContext;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterLockedException;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ConfirmHostRosterServiceTest {

    private static final Long USER_ID = 3L;
    private static final Long MATCH_ID = 100L;
    private static final List<Long> TEAM_MEMBER_IDS = List.of(20L, 21L);
    private static final Game GAME = Game.of(1L, "League of Legends", "MOBA", null);
    private static final GameUserProfile HOST = new GameUserProfile(10L, "host", GAME, USER_ID);

    @Mock
    private LoadHostRosterEditContextPort loadHostRosterEditContextPort;
    @Mock
    private SaveHostRosterPort saveHostRosterPort;
    @Mock
    private FindGameUserUseCase findGameUserUseCase;
    @InjectMocks
    private ConfirmHostRosterService service;

    // 주최자가 전달한 팀원 목록을 저장하고 저장 포트의 경기 결과를 반환한다.
    @Test
    void 주최자가_명단을_확정하면_저장된_경기를_반환한다() {
        // given: 주최자의 경기와 명단 저장 결과를 준비한다.
        FriendlyMatch loaded = match();
        FriendlyMatch saved = match();
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(loaded, false)));
        given(saveHostRosterPort.saveHostRoster(MATCH_ID, TEAM_MEMBER_IDS)).willReturn(saved);
        lenient().when(findGameUserUseCase.findGameUsers(TEAM_MEMBER_IDS))
                .thenReturn(rosterProfiles(TEAM_MEMBER_IDS));

        // when: 요청에 명시한 실제 참가자 두 명으로 명단을 확정한다.
        FriendlyMatch result = service.confirm(
                ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, TEAM_MEMBER_IDS));

        // then: 입력 목록을 그대로 저장하고 팀별 최대 인원 5인 저장 결과를 반환한다.
        then(saveHostRosterPort).should().saveHostRoster(MATCH_ID, TEAM_MEMBER_IDS);
        then(saveHostRosterPort).shouldHaveNoMoreInteractions();
        assertThat(result).isSameAs(saved);
        assertThat(result.capacity()).isEqualTo(5);
    }

    // 대상 경기가 없으면 명단을 저장하지 않는다.
    @Test
    void 경기가_없으면_예외를_던지고_저장하지_않는다() {
        // given: 대상 경기 조회 결과가 없는 상태를 준비한다.
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.empty());

        // when / then: 명단 확정 시 경기 미존재 예외를 확인하고 저장 호출이 없음을 검증한다.
        assertThatThrownBy(() -> service.confirm(
                ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, TEAM_MEMBER_IDS)))
                .isInstanceOf(FriendlyMatchNotFoundException.class);
        then(saveHostRosterPort).shouldHaveNoInteractions();
    }

    // 인증 사용자가 실제 주최자가 아니면 명단을 저장하지 않는다.
    @Test
    void 주최자가_아니면_접근을_거부하고_저장하지_않는다() {
        // given: 사용자 3이 주최하는 경기를 준비한다.
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(match(), false)));

        // when / then: 사용자 4의 확정 요청을 거부하고 저장 호출이 없음을 검증한다.
        assertThatThrownBy(() -> service.confirm(
                ConfirmHostRosterCommand.of(4L, MATCH_ID, TEAM_MEMBER_IDS)))
                .isInstanceOf(HostRosterAccessDeniedException.class);
        then(saveHostRosterPort).shouldHaveNoInteractions();
        verifyNoInteractions(findGameUserUseCase);
    }

    // 같은 참가자 프로필을 두 번 지정하면 저장을 막는다.
    @Test
    void 중복된_명단이면_저장하지_않는다() {
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(match(), false)));

        assertThatThrownBy(() -> service.confirm(
                ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, List.of(20L, 20L))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveHostRosterPort);
    }

    // 참가자 조회의 미존재 예외를 그대로 전달하고 저장을 막는다.
    @Test
    void 명단의_게임_프로필이_없으면_저장하지_않는다() {
        List<Long> ids = List.of(20L, 99L);
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(match(), false)));
        lenient().when(findGameUserUseCase.findGameUsers(ids)).thenThrow(new GameUserNotFoundException());

        assertThatThrownBy(() -> service.confirm(ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, ids)))
                .isInstanceOf(GameUserNotFoundException.class);
        verifyNoInteractions(saveHostRosterPort);
    }

    // 다른 게임의 프로필이 명단에 섞이면 저장을 막는다.
    @Test
    void 명단에_다른_게임_프로필이_있으면_저장하지_않는다() {
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(match(), false)));
        Game otherGame = Game.of(2L, "Other Game", "FPS", null);
        lenient().when(findGameUserUseCase.findGameUsers(TEAM_MEMBER_IDS))
                .thenReturn(List.of(new GameUserProfile(20L, "member20", GAME, 4L),
                        new GameUserProfile(21L, "member21", otherGame, 5L)));

        assertThatThrownBy(() -> service.confirm(
                ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, TEAM_MEMBER_IDS)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveHostRosterPort);
    }

    // 두 모집 방식에서 최대 인원을 넘는 실제 참가자 명단을 거절한다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 명단이_최대_인원을_초과하면_저장하지_않는다(FriendlyMatchSort sort) {
        List<Long> ids = List.of(20L, 21L, 22L, 23L, 24L, 25L);
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(match(sort), false)));
        lenient().when(findGameUserUseCase.findGameUsers(ids)).thenReturn(rosterProfiles(ids));

        assertThatThrownBy(() -> service.confirm(ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, ids)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saveHostRosterPort);
    }

    // 명단에 주최자가 없어도 실제 참가자가 최대 인원과 같으면 저장한다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 명단이_최대_인원과_같으면_저장한다(FriendlyMatchSort sort) {
        List<Long> ids = List.of(20L, 21L, 22L, 23L, 24L);
        FriendlyMatch saved = match(sort);
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(saved, false)));
        lenient().when(findGameUserUseCase.findGameUsers(ids)).thenReturn(rosterProfiles(ids));
        given(saveHostRosterPort.saveHostRoster(MATCH_ID, ids)).willReturn(saved);

        assertThat(service.confirm(ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, ids))).isSameAs(saved);
        then(saveHostRosterPort).should().saveHostRoster(MATCH_ID, ids);
    }

    // 빈 명단도 두 모집 방식에서 확정하여 저장할 수 있다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 빈_명단도_확정하여_저장한다(FriendlyMatchSort sort) {
        List<Long> ids = List.of();
        FriendlyMatch saved = match(sort);
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID)).willReturn(Optional.of(new HostRosterEditContext(saved, false)));
        given(saveHostRosterPort.saveHostRoster(MATCH_ID, ids)).willReturn(saved);

        assertThat(service.confirm(ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, ids))).isSameAs(saved);
        then(saveHostRosterPort).should().saveHostRoster(MATCH_ID, ids);
    }

    // 모집이 열려 있는 개인전도 첫 승인 기록이 있으면 명단을 고정한다.
    @ParameterizedTest
    @EnumSource(FriendlyMatchSort.class)
    void 승인_기록이_있으면_명단_검증과_저장_전에_거부한다(FriendlyMatchSort sort) {
        // given: 빈 명단이나 잘못된 명단을 보내도 승인 후 변경은 허용하지 않는다.
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID))
                .willReturn(Optional.of(new HostRosterEditContext(match(sort), true)));

        // when / then: 프로필 조회와 저장 없이 승인 후 변경 충돌을 반환한다.
        assertThatThrownBy(() -> service.confirm(ConfirmHostRosterCommand.of(USER_ID, MATCH_ID, List.of())))
                .isInstanceOf(HostRosterLockedException.class);
        verifyNoInteractions(findGameUserUseCase, saveHostRosterPort);
    }

    // 승인 완료 여부보다 권한 오류를 먼저 처리한다.
    @Test
    void 승인_후에도_타인은_409가_아니라_권한_예외를_받는다() {
        given(loadHostRosterEditContextPort.loadForUpdate(MATCH_ID))
                .willReturn(Optional.of(new HostRosterEditContext(match(), true)));

        assertThatThrownBy(() -> service.confirm(ConfirmHostRosterCommand.of(4L, MATCH_ID, TEAM_MEMBER_IDS)))
                .isInstanceOf(HostRosterAccessDeniedException.class);
        verifyNoInteractions(findGameUserUseCase, saveHostRosterPort);
    }

    // 명단의 각 식별자에 대응하는 같은 게임 프로필을 준비한다.
    private List<GameUserProfile> rosterProfiles(List<Long> ids) {
        return ids.stream().map(id -> new GameUserProfile(id, "member" + id, GAME, id)).toList();
    }

    // 실제 참가자 명단과 별개로 팀별 최대 인원이 5인 TEAM 경기를 만든다.
    private FriendlyMatch match() {
        return match(FriendlyMatchSort.TEAM);
    }

    private FriendlyMatch match(FriendlyMatchSort sort) {
        return new FriendlyMatch(
                MATCH_ID, HOST, GAME, LocalDate.of(2026, 12, 1), LocalTime.of(19, 0),
                sort, FriendlyMatchState.RECRUITING, 5, "친선 경기");
    }
}

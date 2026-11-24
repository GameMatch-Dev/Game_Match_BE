package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.HostRosterAccessDeniedException;
import com.hd.gamematch.friendlymatch.application.port.in.hostroster.ConfirmHostRosterCommand;
import com.hd.gamematch.friendlymatch.application.port.out.LoadFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveHostRosterPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
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

@ExtendWith(MockitoExtension.class)
class ConfirmHostRosterServiceTest {

    private static final Long USER_ID = 3L;
    private static final Long MATCH_ID = 100L;
    private static final List<Long> TEAM_MEMBER_IDS = List.of(20L, 21L);
    private static final Game GAME = Game.of(1L, "League of Legends", "MOBA", null);
    private static final GameUserProfile HOST = new GameUserProfile(10L, "host", GAME, USER_ID);

    @Mock
    private LoadFriendlyMatchPort loadFriendlyMatchPort;
    @Mock
    private SaveHostRosterPort saveHostRosterPort;
    @InjectMocks
    private ConfirmHostRosterService service;

    // 주최자가 전달한 팀원 목록을 저장하고 저장 포트의 경기 결과를 반환한다.
    @Test
    void 주최자가_명단을_확정하면_저장된_경기를_반환한다() {
        // given: 주최자의 경기와 명단 저장 결과를 준비한다.
        FriendlyMatch loaded = match();
        FriendlyMatch saved = match();
        given(loadFriendlyMatchPort.loadFriendlyMatch(MATCH_ID)).willReturn(Optional.of(loaded));
        given(saveHostRosterPort.saveHostRoster(MATCH_ID, TEAM_MEMBER_IDS)).willReturn(saved);

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
        given(loadFriendlyMatchPort.loadFriendlyMatch(MATCH_ID)).willReturn(Optional.empty());

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
        given(loadFriendlyMatchPort.loadFriendlyMatch(MATCH_ID)).willReturn(Optional.of(match()));

        // when / then: 사용자 4의 확정 요청을 거부하고 저장 호출이 없음을 검증한다.
        assertThatThrownBy(() -> service.confirm(
                ConfirmHostRosterCommand.of(4L, MATCH_ID, TEAM_MEMBER_IDS)))
                .isInstanceOf(HostRosterAccessDeniedException.class);
        then(saveHostRosterPort).shouldHaveNoInteractions();
    }

    // 실제 참가자 명단과 별개로 팀별 최대 인원이 5인 TEAM 경기를 만든다.
    private FriendlyMatch match() {
        return new FriendlyMatch(
                MATCH_ID, HOST, GAME, LocalDate.of(2026, 12, 1), LocalTime.of(19, 0),
                FriendlyMatchSort.TEAM, FriendlyMatchState.RECRUITING, 5, "친선 경기");
    }
}

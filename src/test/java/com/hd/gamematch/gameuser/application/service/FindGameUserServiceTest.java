package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUsersPort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FindGameUserServiceTest {

    @Mock
    private LoadGameUserPort loadGameUserPort;

    @Mock
    private LoadGameUsersPort loadGameUsersPort;

    @InjectMocks
    private FindGameUserService findGameUserService;

    @Test
    void 존재하는_게임_프로필을_조회한다() {
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );
        given(loadGameUserPort.loadGameUserById(1L)).willReturn(Optional.of(profile));

        GameUserProfile result = findGameUserService.findGameUser(FindGameUserQuery.of(1L));

        assertThat(result).isEqualTo(profile);
    }

    @Test
    void 존재하지_않는_게임_프로필을_조회하면_전용_예외를_던진다() {
        given(loadGameUserPort.loadGameUserById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> findGameUserService.findGameUser(FindGameUserQuery.of(999L)))
                .isInstanceOf(GameUserNotFoundException.class)
                .hasMessage("게임 프로필을 찾을 수 없습니다.");
    }

    // 저장소 반환 순서와 무관하게 요청한 프로필 순서를 유지한다.
    @Test
    void 일괄_조회한_프로필을_요청한_ID_순서로_반환한다() {
        Game game = Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol");
        GameUserProfile first = new GameUserProfile(1L, "playerA", game, 11L);
        GameUserProfile second = new GameUserProfile(2L, "playerB", game, 12L);
        GameUserProfile third = new GameUserProfile(3L, "playerC", game, 13L);
        List<Long> ids = List.of(3L, 1L, 2L);
        given(loadGameUsersPort.loadGameUsersByIds(ids)).willReturn(List.of(second, third, first));
        FindGameUserUseCase useCase = findGameUserService;

        List<GameUserProfile> result = useCase.findGameUsers(ids);

        assertThat(result).containsExactly(third, first, second);
        verify(loadGameUsersPort).loadGameUsersByIds(ids);
        verifyNoInteractions(loadGameUserPort);
    }

    // 요청한 프로필 일부가 누락되어도 기존 조회 예외를 전달한다.
    @Test
    void 일괄_조회할_프로필이_하나라도_없으면_전용_예외를_던진다() {
        GameUserProfile profile = new GameUserProfile(1L, "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"), 11L);
        List<Long> ids = List.of(1L, 999L);
        given(loadGameUsersPort.loadGameUsersByIds(ids)).willReturn(List.of(profile));
        FindGameUserUseCase useCase = findGameUserService;

        assertThatThrownBy(() -> useCase.findGameUsers(ids))
                .isInstanceOf(GameUserNotFoundException.class)
                .hasMessage("게임 프로필을 찾을 수 없습니다.");
    }

    // 빈 명단은 저장소 조회 없이 반환한다.
    @Test
    void 일괄_조회할_ID가_없으면_포트를_호출하지_않는다() {
        FindGameUserUseCase useCase = findGameUserService;

        assertThat(useCase.findGameUsers(List.of())).isEmpty();

        verifyNoInteractions(loadGameUserPort, loadGameUsersPort);
    }
}

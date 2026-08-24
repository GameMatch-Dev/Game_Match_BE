package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FindGameUserServiceTest {

    @Mock
    private LoadGameUserPort loadGameUserPort;

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
}

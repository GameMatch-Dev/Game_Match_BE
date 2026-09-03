package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.findmyprofile.FindGameUserByUserAndGameQuery;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserByUserAndGamePort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FindGameUserByUserAndGameServiceTest {

    @Mock
    private LoadGameUserByUserAndGamePort loadGameUserByUserAndGamePort;

    @InjectMocks
    private FindGameUserByUserAndGameService findGameUserByUserAndGameService;

    @Test
    void 현재_사용자와_게임에_해당하는_게임_프로필을_조회한다(){
        // given
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );

        given(loadGameUserByUserAndGamePort.loadGameUserByUserIdAndGameId(3L, 2L))
                .willReturn(Optional.of(profile));

        //when
        GameUserProfile result = findGameUserByUserAndGameService.findGameUser(
                FindGameUserByUserAndGameQuery.of(3L, 2L)
        );

        //then
        assertThat(result).isEqualTo(profile);
    }

    @Test
    void 현재_사용자에게_선택한_게임의_프로필이_없으면_예외를_던진다() {
        given(loadGameUserByUserAndGamePort.loadGameUserByUserIdAndGameId(3L, 2L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> findGameUserByUserAndGameService.findGameUser(
                FindGameUserByUserAndGameQuery.of(3L, 2L)
        ))
                .isInstanceOf(GameUserNotFoundException.class)
                .hasMessage("게임 프로필을 찾을 수 없습니다.");
    }
}

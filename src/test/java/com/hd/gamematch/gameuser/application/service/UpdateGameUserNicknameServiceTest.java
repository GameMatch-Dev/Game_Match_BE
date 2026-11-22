package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserAccessDeniedException;
import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.port.in.update.UpdateGameUserNicknameCommand;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserNicknamePort;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.UpdateGameUserNicknamePort;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UpdateGameUserNicknameServiceTest {

    @Mock
    private LoadGameUserPort loadGameUserPort;

    @Mock
    private UpdateGameUserNicknamePort updateGameUserNicknamePort;

    @Mock
    private ExistsGameUserNicknamePort existsGameUserNicknamePort;

    @InjectMocks
    private UpdateGameUserNicknameService updateGameUserNicknameService;

    @Test
    void 인증된_소유자가_게임_프로필_닉네임을_수정한다() {
        // given
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );
        given(loadGameUserPort.loadGameUserById(1L)).willReturn(Optional.of(profile));

        // when
        GameUserProfile result = updateGameUserNicknameService.updateNickname(
                UpdateGameUserNicknameCommand.of(3L, 1L, "newPlayer")
        );

        // then
        assertThat(result.nickname()).isEqualTo("newPlayer");
        assertThat(result.game()).isEqualTo(profile.game());
        assertThat(result.userId()).isEqualTo(3L);
        then(existsGameUserNicknamePort).should()
                .existsByGameIdAndNickname(2L, "newPlayer");
        then(updateGameUserNicknamePort).should().updateNickname(1L, "newPlayer");
    }

    @Test
    void 같은_게임의_다른_사용자가_사용_중인_닉네임으로_수정하면_중복_예외를_던지고_수정하지_않는다() {
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );
        given(loadGameUserPort.loadGameUserById(1L)).willReturn(Optional.of(profile));
        given(existsGameUserNicknamePort.existsByGameIdAndNickname(2L, "duplicatedName"))
                .willReturn(true);

        assertThatThrownBy(() -> updateGameUserNicknameService.updateNickname(
                UpdateGameUserNicknameCommand.of(3L, 1L, "duplicatedName")
        ))
                .isInstanceOf(GameUserNicknameAlreadyInUseException.class)
                .hasMessage("이미 해당 게임에서 사용 중인 닉네임입니다.");

        then(existsGameUserNicknamePort).should()
                .existsByGameIdAndNickname(2L, "duplicatedName");
        then(updateGameUserNicknamePort).shouldHaveNoInteractions();
    }

    @Test
    void 다른_사용자가_게임_프로필_닉네임을_수정하면_권한_예외를_던지고_수정하지_않는다() {
        // given: 프로필 주인은 3번 사용자지만, 4번 사용자가 수정 요청을 보낸다.
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );
        given(loadGameUserPort.loadGameUserById(1L)).willReturn(Optional.of(profile));

        // when & then: 소유자 검증은 DB 수정 Adapter를 호출하기 전에 끝나야 한다.
        assertThatThrownBy(() -> updateGameUserNicknameService.updateNickname(
                UpdateGameUserNicknameCommand.of(4L, 1L, "hijackedName")
        ))
                .isInstanceOf(GameUserAccessDeniedException.class)
                .hasMessage("게임 프로필을 수정할 권한이 없습니다.");

        then(updateGameUserNicknamePort).should(never()).updateNickname(1L, "hijackedName");
    }

    @Test
    void 존재하지_않는_게임_프로필_닉네임을_수정하면_조회_예외를_던지고_수정하지_않는다() {
        // Optional.empty(): 조회 조건에 맞는 게임 프로필 행이 DB에 없다는 뜻이다.
        given(loadGameUserPort.loadGameUserById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> updateGameUserNicknameService.updateNickname(
                UpdateGameUserNicknameCommand.of(3L, 999L, "newPlayer")
        ))
                .isInstanceOf(GameUserNotFoundException.class)
                .hasMessage("게임 프로필을 찾을 수 없습니다.");

        then(updateGameUserNicknamePort).shouldHaveNoInteractions();
    }

    @Test
    void 기존과_같은_닉네임으로_수정하면_기존_프로필을_반환하고_수정하지_않는다() {
        GameUserProfile profile = new GameUserProfile(
                1L,
                "playerA",
                Game.of(2L, "League of Legends", "MOBA", "https://example.com/lol"),
                3L
        );
        given(loadGameUserPort.loadGameUserById(1L)).willReturn(Optional.of(profile));

        GameUserProfile result = updateGameUserNicknameService.updateNickname(
                UpdateGameUserNicknameCommand.of(3L, 1L, "playerA")
        );

        assertThat(result).isEqualTo(profile);
        then(existsGameUserNicknamePort).shouldHaveNoInteractions();
        then(updateGameUserNicknamePort).shouldHaveNoInteractions();
    }
}

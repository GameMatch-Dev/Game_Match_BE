package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserCommand;
import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserNicknamePort;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.SaveGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
class RegisterGameUserServiceTest {

    // 실제 DB 저장 여부는 다음 단계의 JPA 통합 테스트가 검사한다.
    @Mock
    private SaveGameUserPort saveGameUserPort;

    @Mock
    private ExistsGameUserPort existsGameUserPort;

    @InjectMocks
    private RegisterGameUserService registerGameUserService;

    @Captor
    private ArgumentCaptor<GameUser> gameUserCaptor;

    @Mock
    private ExistsGameUserNicknamePort existsGameUserNicknamePort;

    @Test
    void registerGameUser() {
        // Given
        RegisterGameUserCommand command = new RegisterGameUserCommand(
                1L,
                10L,
                "playerA"
        );
        when(saveGameUserPort.save(any(GameUser.class))).thenReturn(100L);

        // When
        // Service가 저장을 요청할 외부 저장소 역할의 Port를 호출한다.
        // 이 테스트에서는 실제 DB 대신 Mock SaveGameUserPort를 사용한다.
        Long gameUserId = registerGameUserService.register(command);

        // Then
        // 1. save(...)가 호출됐는지 확인하고, 그때 전달된 GameUser를 Captor가 잡는다.
        verify(saveGameUserPort).save(gameUserCaptor.capture());
        // 2. Captor가 잡아 둔 GameUser를 꺼낸다.
        GameUser savedGameUser = gameUserCaptor.getValue();
        assertThat(savedGameUser.getUserId()).isEqualTo(1L);
        assertThat(savedGameUser.getGameId()).isEqualTo(10L);
        assertThat(savedGameUser.getNickname()).isEqualTo("playerA");
        assertThat(gameUserId).isEqualTo(100L);

    }

    @Test
    void registerGameUserRejectsWhenUserAlreadyRegisteredForGame() {
        // Given: 이미 같은 게임에 등록된 사용자라고 가정한다.
        RegisterGameUserCommand command = new RegisterGameUserCommand(
                1L,
                10L,
                "playerA"
        );

        when(existsGameUserPort.existsByUserIdAndGameId(1L, 10L))
                .thenReturn(true);

        // When & Then: 중복 등록은 거부하고 저장을 요청하지 않는다.
        assertThatThrownBy(() -> registerGameUserService.register(command))
                .isInstanceOf(GameUserAlreadyRegisteredException.class);

        verifyNoInteractions(saveGameUserPort);
    }

    @Test
    void registerGameUserRejectsWhenNicknameIsAlreadyUsedForGame() {
        // Given: 다른 사용자가 같은 게임에서 이미 playerA 닉네임을 사용 중이다.
        RegisterGameUserCommand command = new RegisterGameUserCommand(
                2L,
                10L,
                "playerA"
        );

        // 검사용 포트 -> 다른 사용자가 playerA를 등록을 시도할 때 이미 존재한다고 알린다고 응답을 설정
        when(existsGameUserNicknamePort.existsByGameIdAndNickname(
                10L,
                "playerA"
        )).thenReturn(true);

        // When & Then: 새 프로필을 저장하지 않고 닉네임 중복 예외를 던진다.
        // 즉 테스트 내용은
        // 이미 사용 중이라고 들었을 때
        // -> Service가 예외를 던지는가?
        // -> 저장을 시도하지 않는가?
        assertThatThrownBy(() -> registerGameUserService.register(command))
                .isInstanceOf(GameUserNicknameAlreadyInUseException.class);

        verifyNoInteractions(saveGameUserPort);
    }
}

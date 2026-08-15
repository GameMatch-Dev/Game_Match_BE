package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserCommand;
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
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class RegisterGameUserServiceTest {

    // 실제 DB 저장 여부는 다음 단계의 JPA 통합 테스트가 검사한다.
    @Mock
    private SaveGameUserPort saveGameUserPort;

    @InjectMocks
    private RegisterGameUserService registerGameUserService;

    @Captor
    private ArgumentCaptor<GameUser> gameUserCaptor;

    @Test
    void registerGameUser() {
        // Given
        RegisterGameUserCommand command = new RegisterGameUserCommand(
                1L,
                10L,
                "playerA"
        );

        // When
        // Service가 저장을 요청할 외부 저장소 역할의 Port를 호출한다.
        // 이 테스트에서는 실제 DB 대신 Mock SaveGameUserPort를 사용한다.
        registerGameUserService.register(command);

        // Then
        // 1. save(...)가 호출됐는지 확인하고, 그때 전달된 GameUser를 Captor가 잡는다.
        verify(saveGameUserPort).save(gameUserCaptor.capture());
        // 2. Captor가 잡아 둔 GameUser를 꺼낸다.
        GameUser savedGameUser = gameUserCaptor.getValue();
        assertThat(savedGameUser.getUserId()).isEqualTo(1L);
        assertThat(savedGameUser.getGameId()).isEqualTo(10L);
        assertThat(savedGameUser.getNickname()).isEqualTo("playerA");

    }
}

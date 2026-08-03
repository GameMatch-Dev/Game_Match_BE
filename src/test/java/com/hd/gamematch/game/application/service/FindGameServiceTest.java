package com.hd.gamematch.game.application.service;

import com.hd.gamematch.game.application.port.in.FindGameQuery;
import com.hd.gamematch.game.application.port.out.LoadGamePort;
import com.hd.gamematch.game.application.exception.GameNotFoundException;
import com.hd.gamematch.game.domain.Game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

// Spring 서버를 띄우지 않는 단위 테스트다. Mockito가 가짜 의존성을 준비해 서비스 규칙만 빠르게 확인한다.
@ExtendWith(MockitoExtension.class)
class FindGameServiceTest {

    // 실제 DB 대신 사용할 가짜 LoadGamePort다. 테스트마다 원하는 조회 결과를 미리 정할 수 있다.
    @Mock
    private LoadGamePort loadGamePort;

    // Mockito가 위 가짜 포트를 생성자로 넣어, 테스트 대상인 FindGameService를 만들어 준다.
    @InjectMocks
    private FindGameService findGameService;

    @Test
    void findGameById() {
        // given: 포트가 게임을 찾았다고 가정한다.
        Game game = Game.of(
                1L,
                "League of Legends",
                "MOBA",
                "https://example.com/lol"
        );
        given(loadGamePort.loadGameById(1L))
                .willReturn(Optional.of(game));

        // when: 서비스를 호출하고, then: 반환된 게임 정보와 포트 호출 여부를 함께 검증한다.
        Game result = findGameService.findGame(FindGameQuery.of(1L));

        assertEquals(1L, result.id());
        assertEquals("League of Legends", result.name());
        assertEquals("MOBA", result.sort());
        assertEquals("https://example.com/lol", result.url());

        then(loadGamePort).should().loadGameById(1L);
    }

    @Test
    void throwsGameNotFoundExceptionWhenGameDoesNotExist() {
        // given: Optional.empty()는 저장소에 해당 ID의 게임이 없다는 상황을 뜻한다.
        given(loadGamePort.loadGameById(999L))
                .willReturn(Optional.empty());

        // assertThrows는 이 호출에서 기대한 예외가 실제로 발생해야 테스트가 통과하게 한다.
        // 예외 객체도 받아 메시지가 API 계약과 일관된지 확인한다.
        GameNotFoundException exception = assertThrows(
                GameNotFoundException.class,
                () -> findGameService.findGame(FindGameQuery.of(999L))
        );

        assertEquals("게임을 찾을 수 없습니다.", exception.getMessage());
        then(loadGamePort).should().loadGameById(999L);
    }
}

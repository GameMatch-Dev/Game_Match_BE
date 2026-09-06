package com.hd.gamematch.gameuser.application.port.in.findmyprofile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FindGameUserByUserAndGameQueryTest {

    @Test
    void 사용자_ID와_게임_ID가_양수이면_정상_생성한다() {
        FindGameUserByUserAndGameQuery query = FindGameUserByUserAndGameQuery.of(1L, 10L);

        assertEquals(1L, query.userId());
        assertEquals(10L, query.gameId());
    }

    @Test
    void 사용자_ID가_0이면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGameUserByUserAndGameQuery.of(0L, 10L)
        );

        assertEquals("userId는 1 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    void 사용자_ID가_음수이면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGameUserByUserAndGameQuery.of(-1L, 10L)
        );

        assertEquals("userId는 1 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    void 게임_ID가_0이면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGameUserByUserAndGameQuery.of(1L, 0L)
        );

        assertEquals("gameId는 1 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    void 게임_ID가_음수이면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGameUserByUserAndGameQuery.of(1L, -1L)
        );

        assertEquals("gameId는 1 이상이어야 합니다.", exception.getMessage());
    }
}

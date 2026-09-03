package com.hd.gamematch.gameuser.application.port.in.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchGameUsersQueryTest {

    @Test
    void 검색_조건이_정상이면_공백을_제거해_생성한다() {
        SearchGameUsersQuery query = SearchGameUsersQuery.of(" player ", 2L, 1, 10);

        assertEquals("player", query.nickname());
        assertEquals(2L, query.gameId());
        assertEquals(1, query.page());
        assertEquals(10, query.size());
    }

    @Test
    void 닉네임이_null이면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SearchGameUsersQuery.of(null, null, 1, 10)
        );

        assertEquals("nickname은 필수입니다.", exception.getMessage());
    }

    @Test
    void 닉네임이_두_글자_미만이면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SearchGameUsersQuery.of(" a ", null, 1, 10)
        );

        assertEquals("nickname은 2글자 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    void 게임_ID가_0_이하면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SearchGameUsersQuery.of("player", 0L, 1, 10)
        );

        assertEquals("gameId는 1 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    void 페이지가_1_미만이면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SearchGameUsersQuery.of("player", null, 0, 10)
        );

        assertEquals("page는 1 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    void 페이지_크기가_범위를_벗어나면_거부한다() {
        IllegalArgumentException tooSmall = assertThrows(
                IllegalArgumentException.class,
                () -> SearchGameUsersQuery.of("player", null, 1, 0)
        );
        IllegalArgumentException tooLarge = assertThrows(
                IllegalArgumentException.class,
                () -> SearchGameUsersQuery.of("player", null, 1, 21)
        );

        assertEquals("size는 1 이상 20 이하여야 합니다.", tooSmall.getMessage());
        assertEquals("size는 1 이상 20 이하여야 합니다.", tooLarge.getMessage());
    }
}

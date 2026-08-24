package com.hd.gamematch.game.application.port.in;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FindGamesQueryTest {

    @Test
    void 이름과_정렬값의_앞뒤_공백을_제거한다() {
        FindGamesQuery query = FindGamesQuery.of(" League of Legends ", " MOBA ");

        assertTrue(query.hasName());
        assertTrue(query.hasSort());
        assertEquals("League of Legends", query.name());
        assertEquals("MOBA", query.sort());
    }

    @Test
    void 빈_값은_널로_정규화한다() {
        FindGamesQuery query = FindGamesQuery.of(" ", "\t");

        assertFalse(query.hasName());
        assertFalse(query.hasSort());
        assertNull(query.name());
        assertNull(query.sort());
    }

    @Test
    void 공백_제거_후_이름이_백_글자를_초과하면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of(" " + "a".repeat(101) + " ", null)
        );

        assertEquals("name은 100자 이하여야 합니다.", exception.getMessage());
    }

    @Test
    void 이름에_제어_문자가_있으면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of("League\nof Legends", null)
        );

        assertEquals("name에는 제어문자를 포함할 수 없습니다.", exception.getMessage());
    }

    @Test
    void 공백_제거_후_정렬값이_오십_글자를_초과하면_거부한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of(null, " " + "a".repeat(51) + " ")
        );

        assertEquals("sort는 50자 이하여야 합니다.", exception.getMessage());
    }

    @Test
    void 이름과_정렬값이_모두_유효하지_않으면_이름을_먼저_검증한다() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of("a".repeat(101), "a".repeat(51))
        );

        assertEquals("name은 100자 이하여야 합니다.", exception.getMessage());
    }
}

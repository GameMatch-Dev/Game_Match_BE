package com.hd.gamematch.game.application.port.in;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FindGamesQueryTest {

    @Test
    void nameAndSortAreTrimmed() {
        FindGamesQuery query = FindGamesQuery.of(" League of Legends ", " MOBA ");

        assertTrue(query.hasName());
        assertTrue(query.hasSort());
        assertEquals("League of Legends", query.name());
        assertEquals("MOBA", query.sort());
    }

    @Test
    void blankValuesAreNormalizedToNull() {
        FindGamesQuery query = FindGamesQuery.of(" ", "\t");

        assertFalse(query.hasName());
        assertFalse(query.hasSort());
        assertNull(query.name());
        assertNull(query.sort());
    }

    @Test
    void rejectsNameLongerThanOneHundredCharactersAfterTrimming() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of(" " + "a".repeat(101) + " ", null)
        );

        assertEquals("name은 100자 이하여야 합니다.", exception.getMessage());
    }

    @Test
    void rejectsNameContainingControlCharacter() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of("League\nof Legends", null)
        );

        assertEquals("name에는 제어문자를 포함할 수 없습니다.", exception.getMessage());
    }

    @Test
    void rejectsSortLongerThanFiftyCharactersAfterTrimming() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of(null, " " + "a".repeat(51) + " ")
        );

        assertEquals("sort는 50자 이하여야 합니다.", exception.getMessage());
    }

    @Test
    void validatesNameBeforeSortWhenBothValuesAreInvalid() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindGamesQuery.of("a".repeat(101), "a".repeat(51))
        );

        assertEquals("name은 100자 이하여야 합니다.", exception.getMessage());
    }
}

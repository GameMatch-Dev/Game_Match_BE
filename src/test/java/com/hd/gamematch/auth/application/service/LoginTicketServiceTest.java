package com.hd.gamematch.auth.application.service;

import com.hd.gamematch.auth.application.exception.InvalidLoginTicketException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginTicketServiceTest {

    @Test
    void consumeRejectsUnknownTicket() {
        LoginTicketService loginTicketService = new LoginTicketService(new MutableClock());

        assertThrows(InvalidLoginTicketException.class,
                () -> loginTicketService.consume("unknown-ticket"));
    }

    @Test
    void consumeRejectsTicketThatWasAlreadyUsed() {
        LoginTicketService loginTicketService = new LoginTicketService(new MutableClock());
        String ticket = loginTicketService.issue(3L);

        assertEquals(3L, loginTicketService.consume(ticket));
        assertThrows(InvalidLoginTicketException.class,
                () -> loginTicketService.consume(ticket));
    }

    @Test
    void consumeRejectsExpiredTicket() {
        MutableClock clock = new MutableClock();
        LoginTicketService loginTicketService = new LoginTicketService(clock);
        String ticket = loginTicketService.issue(3L);

        clock.advance(Duration.ofMinutes(1));

        assertThrows(InvalidLoginTicketException.class,
                () -> loginTicketService.consume(ticket));
    }

    private static final class MutableClock extends Clock {

        private Instant current = Instant.parse("2026-08-07T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}

package com.hd.gamematch.auth.application.service;

import com.hd.gamematch.auth.application.exception.InvalidLoginTicketException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginTicketService {

    private static final Duration TICKET_TTL = Duration.ofMinutes(1);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, LoginTicket> tickets = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginTicketService() {
        this(Clock.systemUTC());
    }

    LoginTicketService(Clock clock) {
        this.clock = clock;
    }

    public String issue(Long userId) {
        String ticket = randomUrlSafeValue();
        tickets.put(ticket, new LoginTicket(userId, clock.instant().plus(TICKET_TTL)));
        return ticket;
    }

    /**
     * 티켓은 JWT와 달리 서버가 한 번 소비하면 바로 무효화할 수 있다.
     * 그래서 프런트 redirect URL에 access token 자체를 넣지 않는다.
     */
    public Long consume(String ticket) {
        LoginTicket loginTicket = tickets.remove(ticket);
        if (loginTicket == null || !loginTicket.expiresAt().isAfter(clock.instant())) {
            throw new InvalidLoginTicketException();
        }
        return loginTicket.userId();
    }

    private String randomUrlSafeValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record LoginTicket(Long userId, Instant expiresAt) {
    }
}

package com.hd.gamematch.friendlymatch.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FriendlyMatchTimeConfiguration {
    // 기존 서버의 로컬 일시 기준을 유지하며 테스트에서는 고정 Clock으로 대체한다.
    @Bean
    Clock friendlyMatchClock() {
        return Clock.systemDefaultZone();
    }
}

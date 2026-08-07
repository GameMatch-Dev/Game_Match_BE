package com.hd.gamematch.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /** 인증 실패도 CommonResponse JSON으로 반환하기 위해 보안 계층용 ObjectMapper를 등록한다. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

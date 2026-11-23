package com.hd.gamematch.friendlymatch.adapter.in.web.request;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

// 애너테이션을 붙인 정수 필드에만 소수 입력 거부를 적용한다.
public final class FriendlyMatchIntegerDeserializers {
    private FriendlyMatchIntegerDeserializers() {}

    public static final class LongValue extends ValueDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return context.reportInputMismatch(this, "정수 필드에는 소수를 입력할 수 없습니다.");
            }
            // 범위 검사와 나머지 기본 파싱 규칙은 Jackson에 맡긴다.
            return context.readValue(parser, Long.class);
        }
    }

    public static final class IntegerValue extends ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return context.reportInputMismatch(this, "정수 필드에는 소수를 입력할 수 없습니다.");
            }
            return context.readValue(parser, Integer.class);
        }
    }
}

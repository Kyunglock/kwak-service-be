package com.investment.portal.insight;

import com.investment.portal.infrastructure.external.kwakai.JsonExtractor;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JsonExtractorTest {

    @Test
    void extractsPlainJson() {
        assertThat(JsonExtractor.extractJsonObject("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void extractsFromCodeFenceAndProse() {
        String raw = "분석 결과입니다:\n```json\n{\"a\":1,\"b\":[1,2]}\n```\n감사합니다.";
        assertThat(JsonExtractor.extractJsonObject(raw)).isEqualTo("{\"a\":1,\"b\":[1,2]}");
    }

    @Test
    void returnsNullWhenNoBraces() {
        assertThat(JsonExtractor.extractJsonObject("설명만 있고 JSON 없음")).isNull();
        assertThat(JsonExtractor.extractJsonObject(null)).isNull();
    }
}

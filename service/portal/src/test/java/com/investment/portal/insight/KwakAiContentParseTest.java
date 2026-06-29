package com.investment.portal.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.portal.infrastructure.external.kwakai.KwakAiClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KwakAiContentParseTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void extractsAssistantContent() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"a\\\":1}\"}}]}";
        assertThat(KwakAiClient.parseAssistantContent(om.readTree(body))).isEqualTo("{\"a\":1}");
    }

    @Test
    void returnsNullWhenNoChoices() throws Exception {
        assertThat(KwakAiClient.parseAssistantContent(om.readTree("{\"choices\":[]}"))).isNull();
        assertThat(KwakAiClient.parseAssistantContent(null)).isNull();
    }
}

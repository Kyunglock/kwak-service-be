package com.investment.portal.infrastructure.external.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 인사이트 분석용 OpenAI gpt-4o-mini 클라이언트
 * API 키 미설정 시 null 반환 → 호출 측에서 규칙 기반 폴백 처리
 */
@Slf4j
@Component
public class InsightOpenAiClient {

    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public InsightOpenAiClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().baseUrl(BASE_URL).build();
    }

    /**
     * GPT 호출 후 응답 content 줄 목록 반환.
     * 응답 포맷: {"lines": ["줄1", "줄2", ...]}
     *
     * @return 줄 목록, 키 미설정/오류 시 null
     */
    public List<String> chatLines(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("[InsightAI] OPENAI_API_KEY 미설정 — 규칙 기반 폴백 사용");
            return null;
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "response_format", Map.of("type", "json_object"),
                    "max_tokens", 800,
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String raw = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            JsonNode root    = objectMapper.readTree(raw);
            String   content = root.path("choices").get(0).path("message").path("content").asText();

            int promptTokens     = root.path("usage").path("prompt_tokens").asInt();
            int completionTokens = root.path("usage").path("completion_tokens").asInt();
            log.info("[InsightAI] 모델={} 입력={}tok 출력={}tok", model, promptTokens, completionTokens);

            JsonNode linesNode = objectMapper.readTree(content).path("lines");
            if (!linesNode.isArray() || linesNode.isEmpty()) {
                log.warn("[InsightAI] 응답 파싱 실패 — lines 배열 없음: {}", content);
                return null;
            }

            List<String> lines = new ArrayList<>();
            linesNode.forEach(n -> lines.add(n.asText()));
            return lines;

        } catch (Exception e) {
            log.warn("[InsightAI] OpenAI 호출 실패 — 규칙 기반 폴백: {}", e.getMessage());
            return null;
        }
    }
}

package com.investment.portal.infrastructure.external.kwakai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.portal.application.dto.kwakai.KwakAiChatRequest;
import com.investment.portal.application.dto.kwakai.KwakAiGenerateRequest;
import com.investment.portal.application.dto.kwakai.KwakAiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KwakAiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(120);
    private static final String SYSTEM_PROMPT =
            "You are a Korean-speaking assistant. " +
            "CRITICAL RULE: You MUST write ALL responses in Korean (한국어) ONLY. " +
            "NEVER use Chinese (中文/汉字), Japanese, or any other language. " +
            "Even if the user writes in another language, ALWAYS respond in Korean. " +
            "If you find yourself writing Chinese characters, STOP and rewrite in Korean. " +
            "당신은 한국어로만 답변하는 AI 어시스턴트입니다. " +
            "절대로 한자(漢字), 중국어, 일본어를 사용하지 마세요. " +
            "모든 답변은 반드시 한국어로만 작성하세요.";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;

    public KwakAiClient(
            @Value("${kwakai.base-url:http://192.168.0.16:8000/v1}") String baseUrl,
            @Value("${kwakai.model:gemma4-31b}") String defaultModel,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.defaultModel = defaultModel;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer dummy")
                .build();
        log.info("[KwakAI] 연결 대상: {}, 모델: {}", baseUrl, defaultModel);
    }

    public JsonNode chat(KwakAiChatRequest request) {
        String model = (request.getModel() != null && !request.getModel().isBlank())
                ? request.getModel() : defaultModel;

        List<KwakAiMessage> messages = new ArrayList<>();
        messages.add(new KwakAiMessage("system", SYSTEM_PROMPT));
        messages.addAll(request.getMessages());

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "stream", false
        );
        return call("/chat/completions", body);
    }

    public JsonNode generate(KwakAiGenerateRequest request) {
        String model = (request.getModel() != null && !request.getModel().isBlank())
                ? request.getModel() : defaultModel;
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        new KwakAiMessage("system", SYSTEM_PROMPT),
                        new KwakAiMessage("user", request.getPrompt())
                ),
                "stream", false
        );
        return call("/chat/completions", body);
    }

    /** 통합 인사이트 호출: 시스템/유저 프롬프트로 1회 호출 후 assistant content 원문 반환. 실패 시 null. */
    public String generateContent(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", defaultModel,
                    "messages", List.of(
                            new KwakAiMessage("system", systemPrompt),
                            new KwakAiMessage("user", userPrompt)
                    ),
                    "stream", false
            );
            return parseAssistantContent(call("/chat/completions", body));
        } catch (Exception e) {
            log.warn("[KwakAI] generateContent 실패: {}", e.getMessage());
            return null;
        }
    }

    public static String parseAssistantContent(JsonNode root) {
        if (root == null) return null;
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) return null;
        String content = choices.get(0).path("message").path("content").asText(null);
        return (content == null || content.isBlank()) ? null : content;
    }

    public JsonNode listModels() {
        try {
            String raw = webClient.get()
                    .uri("/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("[KwakAI] 모델 목록 조회 실패: {}", e.getMessage());
            throw new KwakAiException("모델 목록 조회 실패: " + e.getMessage());
        }
    }

    private JsonNode call(String path, Object requestBody) {
        try {
            String raw = webClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("[KwakAI] {} 호출 실패: {}", path, e.getMessage());
            throw new KwakAiException("LLM 호출 실패: " + e.getMessage());
        }
    }
}

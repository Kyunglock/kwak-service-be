package com.investment.ai.kwakai;

import com.investment.ai.api.dto.VisionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final String visionModel;
    private final Duration visionTimeout;
    private final boolean jsonMode;

    public KwakAiClient(
            @Value("${kwakai.base-url:http://192.168.0.16:8000/v1}") String baseUrl,
            @Value("${kwakai.model:gemma4-31b}") String defaultModel,
            @Value("${kwakai.vision-model:}") String visionModel,
            @Value("${kwakai.vision-timeout-seconds:300}") long visionTimeoutSeconds,
            @Value("${kwakai.json-mode:false}") boolean jsonMode,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.defaultModel = defaultModel;
        // 비전 전용 모델을 따로 띄운 경우에만 지정한다. 비우면 기본 모델을 그대로 쓴다.
        this.visionModel = (visionModel == null || visionModel.isBlank()) ? defaultModel : visionModel;
        // 로컬 GPU에서 고해상도 스크린샷을 처리하면 텍스트 호출보다 훨씬 오래 걸린다
        this.visionTimeout = Duration.ofSeconds(visionTimeoutSeconds);
        this.jsonMode = jsonMode;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer dummy")
                .build();
        log.info("[KwakAI] 연결 대상: {}, 모델: {}, 비전 모델: {}, json-mode: {}",
                baseUrl, defaultModel, this.visionModel, jsonMode);
    }

    /** 구조화 추출용 호출 결과. 토큰 수는 서버가 usage 를 안 주면 0. */
    public record ChatResult(String content, int promptTokens, int completionTokens) {}

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

    /**
     * 구조화 응답(JSON)을 기대하는 호출. 토큰 수까지 돌려준다.
     *
     * <p>generateContent 와 달리 실패를 삼키지 않고 예외를 던진다 —
     * 호출부가 사용자에게 "지금은 못 읽었다"고 알려야 하기 때문이다.
     */
    public ChatResult chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = requestBody(
                defaultModel,
                List.of(
                        Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                        Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));
        return toResult(call("/chat/completions", body, TIMEOUT));
    }

    /**
     * 이미지 + 텍스트 호출.
     *
     * <p>이미지가 없으면 텍스트 전용 호출과 같으므로 chat() 으로 위임한다 —
     * content 를 배열로 보내면 서버 구현에 따라 형식을 까다롭게 받는다.
     *
     * @throws KwakAiException 로컬 모델이 멀티모달이 아니면 서버가 거절한다
     */
    public ChatResult vision(String systemPrompt, String userPrompt, List<VisionRequest.ImagePart> images) {
        if (images == null || images.isEmpty()) {
            return chat(systemPrompt, userPrompt);
        }
        Map<String, Object> body = requestBody(
                visionModel,
                List.of(
                        Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                        Map.of("role", "user", "content",
                                KwakAiVisionSupport.toContentParts(userPrompt, images))));
        return toResult(call("/chat/completions", body, visionTimeout));
    }

    private Map<String, Object> requestBody(String model, List<Map<String, Object>> messages) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        // vLLM의 guided decoding 지원 여부가 버전마다 달라 기본은 끈다.
        // 서버가 지원하면 켜는 편이 파싱 실패가 줄어든다.
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    private ChatResult toResult(JsonNode root) {
        String content = parseAssistantContent(root);
        JsonNode usage = root == null ? null : root.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("completion_tokens").asInt(0);
        return new ChatResult(content, promptTokens, completionTokens);
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
        return call(path, requestBody, TIMEOUT);
    }

    private JsonNode call(String path, Object requestBody, Duration timeout) {
        try {
            String raw = webClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .block();
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("[KwakAI] {} 호출 실패: {}", path, e.getMessage());
            throw new KwakAiException("LLM 호출 실패: " + e.getMessage());
        }
    }
}

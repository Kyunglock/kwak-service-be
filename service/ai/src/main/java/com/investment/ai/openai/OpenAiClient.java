package com.investment.ai.openai;

import com.investment.ai.api.dto.VisionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final OpenAiProperties properties;
    private final RestTemplate restTemplate;

    public ChatResponse chat(String systemPrompt, String userPrompt) {
        return send(List.of(
                Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)
        ));
    }

    /**
     * 이미지 + 텍스트 추론. 응답은 chat() 과 동일하게 JSON 오브젝트로 강제된다.
     *
     * <p>이미지가 하나도 없으면 텍스트 전용 호출과 같아지므로 chat() 으로 위임한다 —
     * content 를 배열로 보내면 일부 모델이 형식을 까다롭게 받는다.
     */
    public ChatResponse vision(String systemPrompt, String userPrompt, List<VisionRequest.ImagePart> images) {
        if (images == null || images.isEmpty()) {
            return chat(systemPrompt, userPrompt);
        }
        return send(List.of(
                Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                Map.of("role", "user", "content", OpenAiVisionSupport.toContentParts(userPrompt, images))
        ));
    }

    @SuppressWarnings("unchecked")
    private ChatResponse send(List<Map<String, Object>> messages) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "response_format", Map.of("type", "json_object"),
                "messages", messages
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        ResponseEntity<Map> response = restTemplate.exchange(
                properties.getBaseUrl() + "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        Map<String, Object> usage = (Map<String, Object>) body.get("usage");
        int promptTokens = ((Number) usage.get("prompt_tokens")).intValue();
        int completionTokens = ((Number) usage.get("completion_tokens")).intValue();

        return new ChatResponse(content, promptTokens, completionTokens);
    }

    public record ChatResponse(String content, int promptTokens, int completionTokens) {}
}

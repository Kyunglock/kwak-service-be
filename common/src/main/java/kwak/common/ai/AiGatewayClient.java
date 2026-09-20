package kwak.common.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** core → ai-app(:8090) 추론 게이트웨이 호출 클라이언트. X-System-Key 로 인증. */
@Component
public class AiGatewayClient {

    public record ChatResponse(String content, int promptTokens, int completionTokens) {}

    /** 비전 요청에 실어 보낼 이미지 한 장. base64 는 data: 접두어 없는 순수 base64. */
    public record ImagePart(String mimeType, String base64) {}

    // 이미지 추론은 텍스트보다 눈에 띄게 느리다 (고해상도 스크린샷 1장에 30초 이상도 나온다)
    private static final Duration VISION_TIMEOUT = Duration.ofSeconds(180);

    private final WebClient webClient;
    private final String systemKey;

    public AiGatewayClient(
            @Value("${ai.base-url:http://localhost:8090}") String baseUrl,
            @Value("${system.api-key:}") String systemKey) {
        this.systemKey = systemKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /** kwakai 로컬 LLM generate → 텍스트 */
    public String generateContent(String system, String user) {
        Map<?, ?> res = webClient.post()
                .uri("/api/v1/ai/kwakai/generate")
                .header("X-System-Key", systemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("system", system == null ? "" : system, "user", user == null ? "" : user))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(120));
        return res == null ? null : String.valueOf(res.get("content"));
    }

    /** OpenAI chat → content + 토큰수 */
    public ChatResponse openaiChat(String system, String user) {
        Map<?, ?> res = webClient.post()
                .uri("/api/v1/ai/openai/chat")
                .header("X-System-Key", systemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("system", system == null ? "" : system, "user", user == null ? "" : user))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(120));
        if (res == null) return new ChatResponse(null, 0, 0);
        return new ChatResponse(
                String.valueOf(res.get("content")),
                res.get("promptTokens") == null ? 0 : ((Number) res.get("promptTokens")).intValue(),
                res.get("completionTokens") == null ? 0 : ((Number) res.get("completionTokens")).intValue());
    }

    /**
     * OpenAI 비전(이미지+텍스트) 호출 → content + 토큰수.
     *
     * <p>로컬 kwakai 로는 보내지 않는다 — 멀티모달 지원이 보장되지 않고,
     * 스크린샷의 작은 숫자를 읽는 정확도가 이 기능의 전부이기 때문이다.
     */
    public ChatResponse openaiVision(String system, String user, List<ImagePart> images) {
        Map<?, ?> res = webClient.post()
                .uri("/api/v1/ai/openai/vision")
                .header("X-System-Key", systemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "system", system == null ? "" : system,
                        "user", user == null ? "" : user,
                        "images", images == null ? List.of() : images))
                .retrieve()
                .bodyToMono(Map.class)
                .block(VISION_TIMEOUT);
        if (res == null) return new ChatResponse(null, 0, 0);
        return new ChatResponse(
                String.valueOf(res.get("content")),
                res.get("promptTokens") == null ? 0 : ((Number) res.get("promptTokens")).intValue(),
                res.get("completionTokens") == null ? 0 : ((Number) res.get("completionTokens")).intValue());
    }
}

package kwak.common.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/** core → ai-app(:8090) 추론 게이트웨이 호출 클라이언트. X-System-Key 로 인증. */
@Component
public class AiGatewayClient {

    public record ChatResponse(String content, int promptTokens, int completionTokens) {}

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
}

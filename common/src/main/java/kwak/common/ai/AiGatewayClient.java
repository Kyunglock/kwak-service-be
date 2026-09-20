package kwak.common.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * core → ai-app(:8090) 추론 게이트웨이 호출 클라이언트. X-System-Key 로 인증.
 *
 * <p>모든 추론은 로컬 LLM(kwakai)으로 간다. 외부 LLM 벤더 연동은 두지 않는다.
 */
@Component
public class AiGatewayClient {

    public record ChatResponse(String content, int promptTokens, int completionTokens) {}

    /** 비전 요청에 실어 보낼 이미지 한 장. base64 는 data: 접두어 없는 순수 base64. */
    public record ImagePart(String mimeType, String base64) {}

    // 이미지 추론은 텍스트보다 눈에 띄게 느리다. 로컬 GPU에서는 수 분까지 간다.
    // ai 모듈 쪽 상한(kwakai.vision-timeout-seconds, 기본 300초)보다 길게 둔다 —
    // 여기서 먼저 끊기면 서버는 계속 돌고 있는데 사용자만 실패를 본다.
    private static final Duration VISION_TIMEOUT = Duration.ofSeconds(330);

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

    /** 구조화 응답(JSON)을 기대하는 호출 → content + 토큰수 */
    public ChatResponse chat(String system, String user) {
        Map<?, ?> res = webClient.post()
                .uri("/api/v1/ai/chat")
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
     * 비전(이미지+텍스트) 호출 → content + 토큰수.
     *
     * <p>로컬 모델이 멀티모달이 아니면 ai 모듈에서 실패한다.
     * 그 경우 KWAKAI_VISION_MODEL 에 멀티모달 모델을 지정해야 한다.
     */
    public ChatResponse vision(String system, String user, List<ImagePart> images) {
        Map<?, ?> res = webClient.post()
                .uri("/api/v1/ai/vision")
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

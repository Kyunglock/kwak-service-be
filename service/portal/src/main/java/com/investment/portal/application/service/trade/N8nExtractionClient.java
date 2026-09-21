package com.investment.portal.application.service.trade;

import kwak.common.ai.AiGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * n8n의 trade-extract 워크플로우 호출 클라이언트.
 *
 * <p>n8n은 프롬프트와 LLM 호출만 담당하고 <b>원시 응답 문자열</b>을 돌려준다.
 * 파싱·티커 검증·저장은 전부 Java에 남는다 — 그쪽에 테스트가 걸려 있고,
 * LLM이 무엇을 뱉든 사용자 기록이 틀어지면 안 되기 때문이다.
 *
 * <p>여기서 던지는 예외는 {@link TradeExtractionGateway}가 잡아 ai 모듈 직접 호출로
 * 폴백한다. 즉 n8n이 꺼져 있어도 기능은 살아 있다.
 */
@Slf4j
@Component
public class N8nExtractionClient {

    private final WebClient webClient;
    private final String url;
    private final Duration timeout;

    public N8nExtractionClient(
            @Value("${trade.extraction.n8n.url:}") String url,
            @Value("${trade.extraction.n8n.timeout-seconds:360}") long timeoutSeconds) {
        this.url = url;
        // n8n 워크플로우 상한(600초)보다 짧게 둔다 — 여기서 먼저 끊고 폴백하는 편이
        // 사용자를 더 오래 기다리게 하는 것보다 낫다.
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.webClient = WebClient.builder().build();
    }

    /**
     * @param text  자연어 입력 (이미지 요청이면 null)
     * @param image 화면 캡처 (텍스트 요청이면 null)
     * @return LLM 원문 응답
     * @throws IllegalStateException 설정 누락, 응답 없음, 빈 응답
     */
    public String extract(String text, AiGatewayClient.ImagePart image) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("trade.extraction.n8n.url 이 설정되지 않았습니다");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("text", text == null ? "" : text);
        if (image != null) {
            body.put("image", Map.of(
                    "mimeType", image.mimeType() == null ? "image/png" : image.mimeType(),
                    "base64", image.base64()));
        }

        Map<?, ?> res = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);

        if (res == null) {
            throw new IllegalStateException("n8n 응답이 비어 있습니다");
        }
        Object content = res.get("content");
        String text0 = content == null ? null : String.valueOf(content).trim();
        if (text0 == null || text0.isEmpty() || "null".equals(text0)) {
            // 빈 응답을 그대로 흘리면 "매매 내역을 찾지 못했습니다"로 보여, 실패가 정상 결과로 둔갑한다.
            // 실패로 처리해 폴백을 태운다.
            throw new IllegalStateException("n8n 이 빈 추출 결과를 반환했습니다");
        }
        return text0;
    }
}

package com.investment.portal.application.service.trade;

import kwak.common.ai.AiGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * 매매 내역 추출 경로 선택기.
 *
 * <p>n8n 워크플로우를 우선 태우고, 실패하면 ai 모듈 직접 호출로 폴백한다.
 * 인프라 하나를 사용자 기능의 단일 장애점으로 만들지 않기 위한 구조다 —
 * n8n은 "켜면 좋아지는 것"이지 "없으면 안 되는 것"이 아니다.
 *
 * <p>프롬프트는 두 곳에 있다: n8n 워크플로우(운영 경로)와 {@link TradeExtractionPrompt}
 * (폴백 경로). 후자는 git 에 남는 기준선 역할도 한다.
 */
@Slf4j
@Component
public class TradeExtractionGateway {

    private final N8nExtractionClient n8nClient;
    private final AiGatewayClient aiGatewayClient;
    private final boolean n8nEnabled;

    public TradeExtractionGateway(
            N8nExtractionClient n8nClient,
            AiGatewayClient aiGatewayClient,
            @Value("${trade.extraction.n8n.enabled:false}") boolean n8nEnabled) {
        this.n8nClient = n8nClient;
        this.aiGatewayClient = aiGatewayClient;
        this.n8nEnabled = n8nEnabled;
        log.info("[TradeCapture] 추출 경로 - n8n 사용: {}", n8nEnabled);
    }

    /** 자연어 문장에서 추출. */
    public String extractFromText(String text) {
        return extract(
                () -> n8nClient.extract(text, null),
                () -> content(aiGatewayClient.chat(
                        TradeExtractionPrompt.SYSTEM,
                        TradeExtractionPrompt.TEXT_USER_PREFIX + (text == null ? "" : text))));
    }

    /** 화면 캡처에서 추출. */
    public String extractFromImage(AiGatewayClient.ImagePart image) {
        return extract(
                () -> n8nClient.extract(null, image),
                () -> content(aiGatewayClient.vision(
                        TradeExtractionPrompt.SYSTEM,
                        TradeExtractionPrompt.IMAGE_USER,
                        List.of(image))));
    }

    private String extract(Supplier<String> viaN8n, Supplier<String> viaAiModule) {
        if (n8nEnabled) {
            try {
                return viaN8n.get();
            } catch (Exception e) {
                // 폴백은 조용히 넘어가면 안 된다 — n8n 이 계속 죽어 있는데
                // 기능이 멀쩡해 보이면 아무도 고치지 않는다.
                log.warn("[TradeCapture] n8n 추출 실패 — ai 모듈로 폴백합니다: {}", e.getMessage());
            }
        }
        try {
            return viaAiModule.get();
        } catch (Exception e) {
            log.error("[TradeCapture] AI 추출 호출 실패", e);
            throw new TradeCaptureUnavailableException(e);
        }
    }

    private String content(AiGatewayClient.ChatResponse response) {
        return response == null ? null : response.content();
    }
}

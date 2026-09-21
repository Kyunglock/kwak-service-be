package com.investment.portal.application.service.trade;

import kwak.common.ai.AiGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** n8n 우선 + ai 모듈 폴백 경로 선택. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeExtractionGatewayTest {

    private static final String N8N_RESULT = "{\"trades\":[{\"name\":\"n8n\"}]}";
    private static final String AI_RESULT = "{\"trades\":[{\"name\":\"ai\"}]}";

    private static final AiGatewayClient.ImagePart IMAGE =
            new AiGatewayClient.ImagePart("image/png", "AQIDBA==");

    @Mock N8nExtractionClient n8nClient;
    @Mock AiGatewayClient aiGatewayClient;

    private TradeExtractionGateway gateway(boolean n8nEnabled) {
        return new TradeExtractionGateway(n8nClient, aiGatewayClient, n8nEnabled);
    }

    private void aiReturns(String content) {
        when(aiGatewayClient.chat(anyString(), anyString()))
                .thenReturn(new AiGatewayClient.ChatResponse(content, 0, 0));
        when(aiGatewayClient.vision(anyString(), anyString(), anyList()))
                .thenReturn(new AiGatewayClient.ChatResponse(content, 0, 0));
    }

    // ── 텍스트 ───────────────────────────────────────────────────────────────────

    @Test
    void n8n이_켜져_있으면_n8n_결과를_쓰고_ai모듈은_부르지_않는다() {
        when(n8nClient.extract("애플 1주", null)).thenReturn(N8N_RESULT);

        assertThat(gateway(true).extractFromText("애플 1주")).isEqualTo(N8N_RESULT);

        verifyNoInteractions(aiGatewayClient);
    }

    @Test
    void n8n이_꺼져_있으면_n8n을_아예_부르지_않는다() {
        aiReturns(AI_RESULT);

        assertThat(gateway(false).extractFromText("애플 1주")).isEqualTo(AI_RESULT);

        verifyNoInteractions(n8nClient);
    }

    @Test
    void n8n이_실패하면_ai모듈로_폴백한다() {
        when(n8nClient.extract(anyString(), isNull()))
                .thenThrow(new IllegalStateException("connection refused"));
        aiReturns(AI_RESULT);

        assertThat(gateway(true).extractFromText("애플 1주")).isEqualTo(AI_RESULT);

        verify(n8nClient).extract("애플 1주", null);
        verify(aiGatewayClient).chat(anyString(), contains("애플 1주"));
    }

    @Test
    void 둘_다_실패하면_재시도_안내로_바뀐다() {
        when(n8nClient.extract(anyString(), isNull()))
                .thenThrow(new IllegalStateException("n8n down"));
        when(aiGatewayClient.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("ai down"));

        assertThatThrownBy(() -> gateway(true).extractFromText("애플 1주"))
                .isInstanceOf(TradeCaptureUnavailableException.class);
    }

    @Test
    void 폴백_경로는_git에_있는_프롬프트를_쓴다() {
        aiReturns(AI_RESULT);

        gateway(false).extractFromText("애플 1주");

        verify(aiGatewayClient).chat(eq(TradeExtractionPrompt.SYSTEM), anyString());
    }

    @Test
    void 입력이_null이어도_폴백_경로가_깨지지_않는다() {
        aiReturns(AI_RESULT);

        assertThat(gateway(false).extractFromText(null)).isEqualTo(AI_RESULT);
    }

    // ── 이미지 ───────────────────────────────────────────────────────────────────

    @Test
    void 이미지도_n8n을_먼저_탄다() {
        when(n8nClient.extract(isNull(), eq(IMAGE))).thenReturn(N8N_RESULT);

        assertThat(gateway(true).extractFromImage(IMAGE)).isEqualTo(N8N_RESULT);

        verifyNoInteractions(aiGatewayClient);
    }

    @Test
    void 이미지_n8n_실패시_ai모듈_비전으로_폴백한다() {
        when(n8nClient.extract(isNull(), any()))
                .thenThrow(new IllegalStateException("timeout"));
        aiReturns(AI_RESULT);

        assertThat(gateway(true).extractFromImage(IMAGE)).isEqualTo(AI_RESULT);

        verify(aiGatewayClient).vision(
                eq(TradeExtractionPrompt.SYSTEM), anyString(), eq(List.of(IMAGE)));
    }

    @Test
    void ai모듈이_null을_돌려주면_그대로_전달한다() {
        // 파서가 빈 목록으로 처리하고 "매매 내역을 찾지 못했습니다"로 안내한다
        when(aiGatewayClient.chat(anyString(), anyString()))
                .thenReturn(new AiGatewayClient.ChatResponse(null, 0, 0));

        assertThat(gateway(false).extractFromText("안녕")).isNull();
    }
}

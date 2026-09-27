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

    // ── 빈 응답 ──────────────────────────────────────────────────────────────────

    @Test
    void ai모듈이_빈_응답을_주면_실패로_다룬다() {
        // 예전에는 null 을 그대로 흘려 "매매 내역을 찾지 못했습니다"로 안내했다.
        // 모델이 아무것도 못 내놓은 것과 화면에 매매가 없는 것이 같은 문구로 보이면
        // 사용자도, 로그를 보는 사람도 원인을 구분할 수 없다.
        when(aiGatewayClient.chat(anyString(), anyString()))
                .thenReturn(new AiGatewayClient.ChatResponse(null, 0, 0));

        assertThatThrownBy(() -> gateway(false).extractFromText("안녕"))
                .isInstanceOf(TradeCaptureUnavailableException.class);
    }

    @Test
    void 이미지_응답이_비어_있어도_실패로_다룬다() {
        when(aiGatewayClient.vision(anyString(), anyString(), anyList()))
                .thenReturn(new AiGatewayClient.ChatResponse("   ", 0, 0));

        assertThatThrownBy(() -> gateway(false).extractFromImage(IMAGE))
                .isInstanceOf(TradeCaptureUnavailableException.class);
    }

    @Test
    void ai모듈이_비면_n8n_결과가_있으면_그쪽을_쓴다() {
        when(n8nClient.extract(anyString(), isNull())).thenReturn(N8N_RESULT);

        assertThat(gateway(true).extractFromText("애플 1주")).isEqualTo(N8N_RESULT);

        verifyNoInteractions(aiGatewayClient);
    }
}

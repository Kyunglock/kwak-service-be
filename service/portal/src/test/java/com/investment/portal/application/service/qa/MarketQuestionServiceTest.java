package com.investment.portal.application.service.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.analyzer.market_analyzer.domain.entity.dividend.DividendHistory;
import com.investment.analyzer.market_analyzer.domain.repository.dividend.DividendHistoryMapper;
import com.investment.portal.application.dto.qa.MarketNewsRow;
import com.investment.portal.application.dto.qa.MarketQuestionRequest;
import com.investment.portal.application.dto.qa.MarketQuestionResponse;
import com.investment.portal.application.dto.stock.StockPriceMoveRow;
import com.investment.portal.application.service.trade.StockResolver;
import com.investment.portal.domain.entity.history.stockPrice.StockPriceHistory;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import com.investment.portal.domain.repository.stock.StockRef;
import kwak.common.ai.AiGatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketQuestionServiceTest {

    @Mock AiGatewayClient aiGatewayClient;
    @Mock StockResolver stockResolver;
    @Mock StockPriceHistoryMapper stockPriceHistoryMapper;
    @Mock DividendHistoryMapper dividendHistoryMapper;
    @Mock MarketNewsLookupService marketNewsLookupService;

    private MarketQuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        // 의도추출 파서 / 기간해석은 실제 구현을 쓴다 — TradeCaptureServiceTest 와 같은 이유로,
        // LLM 응답 -> 질의 파라미터 변환까지가 이 기능의 본체라서 mock 으로 끊으면 안 된다.
        service = new MarketQuestionServiceImpl(
                aiGatewayClient, stockResolver, new PeriodHintResolver(),
                new QuestionIntentParser(new ObjectMapper()),
                stockPriceHistoryMapper, dividendHistoryMapper, marketNewsLookupService);
    }

    private void stubIntent(String questionType, String stockName, String periodHint) {
        when(aiGatewayClient.chat(eq(QuestionIntentExtractionPrompt.SYSTEM), anyString()))
                .thenReturn(new AiGatewayClient.ChatResponse(
                        """
                        {"questionType":"%s","stockName":%s,"periodHint":%s}
                        """.formatted(
                                questionType,
                                stockName == null ? "null" : "\"" + stockName + "\"",
                                periodHint == null ? "null" : "\"" + periodHint + "\""),
                        0, 0));
    }

    private void stubAnswer(String answer) {
        when(aiGatewayClient.chat(eq(MarketAnswerPrompt.SYSTEM), anyString()))
                .thenReturn(new AiGatewayClient.ChatResponse(answer, 0, 0));
    }

    // ── happy path ───────────────────────────────────────────────────────────────

    @Test
    void MAX_DROP_DAY_정상_흐름() {
        stubIntent("MAX_DROP_DAY", "애플", "올해");
        stubAnswer("2026년 3월 11일, 애플은 공급망 경고로 5% 하락했습니다.");
        when(stockResolver.resolve("애플", null))
                .thenReturn(new StockResolver.Resolution("AAPL", "애플", List.of()));
        StockPriceMoveRow moveRow = StockPriceMoveRow.builder()
                .priceDt(LocalDate.of(2026, 3, 11))
                .prevClose(new BigDecimal("250.00"))
                .closePrice(new BigDecimal("237.50"))
                .changePct(new BigDecimal("-5.0000"))
                .build();
        when(stockPriceHistoryMapper.findMaxChangeDay(eq("AAPL"), any(), any(), eq("DROP")))
                .thenReturn(Optional.of(moveRow));
        when(marketNewsLookupService.findNewsForDate(eq("AAPL"), eq("애플"), eq(LocalDate.of(2026, 3, 11))))
                .thenReturn(List.of(MarketNewsRow.builder().title("Apple 급락").source("Reuters").build()));

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("올해 애플 가장 많이 하락했던 날 무슨 일이 있었는지"));

        assertThat(res.questionType()).isEqualTo("MAX_DROP_DAY");
        assertThat(res.stockNm()).isEqualTo("애플");
        assertThat(res.answer()).contains("하락");
        verify(marketNewsLookupService).findNewsForDate("AAPL", "애플", LocalDate.of(2026, 3, 11));
    }

    @Test
    void MAX_GAIN_DAY_정상_흐름은_GAIN_방향으로_쿼리한다() {
        stubIntent("MAX_GAIN_DAY", "삼성전자", "최근 3개월");
        stubAnswer("최근 3개월 중 3월 5일에 가장 많이 올랐습니다.");
        when(stockResolver.resolve("삼성전자", null))
                .thenReturn(new StockResolver.Resolution("005930.KS", "삼성전자", List.of()));
        when(stockPriceHistoryMapper.findMaxChangeDay(eq("005930.KS"), any(), any(), eq("GAIN")))
                .thenReturn(Optional.of(StockPriceMoveRow.builder()
                        .priceDt(LocalDate.of(2026, 3, 5))
                        .prevClose(new BigDecimal("70000"))
                        .closePrice(new BigDecimal("75000"))
                        .changePct(new BigDecimal("7.14"))
                        .build()));
        when(marketNewsLookupService.findNewsForDate(any(), any(), any())).thenReturn(List.of());

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("삼성전자 최근 3개월 중 제일 오른 날"));

        assertThat(res.questionType()).isEqualTo("MAX_GAIN_DAY");
        verify(stockPriceHistoryMapper).findMaxChangeDay(eq("005930.KS"), any(), any(), eq("GAIN"));
    }

    @Test
    void PERIOD_RETURN_정상_흐름() {
        stubIntent("PERIOD_RETURN", "테슬라", "올해");
        stubAnswer("테슬라는 올해 20% 상승했습니다.");
        when(stockResolver.resolve("테슬라", null))
                .thenReturn(new StockResolver.Resolution("TSLA", "테슬라", List.of()));
        when(stockPriceHistoryMapper.findClosestOnOrAfter(eq("TSLA"), any()))
                .thenReturn(Optional.of(priceRow("TSLA", LocalDate.of(2026, 1, 2), "200")));
        when(stockPriceHistoryMapper.findClosestOnOrBefore(eq("TSLA"), any()))
                .thenReturn(Optional.of(priceRow("TSLA", LocalDate.of(2026, 3, 11), "240")));

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("테슬라 올해 수익률 얼마야?"));

        assertThat(res.questionType()).isEqualTo("PERIOD_RETURN");
        // 뉴스 조회는 가격 변동일이 아닌 질문 유형이라 호출되지 않는다
        verifyNoInteractions(marketNewsLookupService);
    }

    @Test
    void PERIOD_HIGH_LOW_정상_흐름() {
        stubIntent("PERIOD_HIGH_LOW", "엔비디아", "올해");
        stubAnswer("엔비디아는 올해 1월에 최고가를 기록했습니다.");
        when(stockResolver.resolve("엔비디아", null))
                .thenReturn(new StockResolver.Resolution("NVDA", "엔비디아", List.of()));
        when(stockPriceHistoryMapper.findPeriodHighDay(eq("NVDA"), any(), any()))
                .thenReturn(Optional.of(priceRow("NVDA", LocalDate.of(2026, 1, 15), "150")));
        when(stockPriceHistoryMapper.findPeriodLowDay(eq("NVDA"), any(), any()))
                .thenReturn(Optional.of(priceRow("NVDA", LocalDate.of(2026, 2, 1), "110")));

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("엔비디아 올해 최고가 최저가"));

        assertThat(res.questionType()).isEqualTo("PERIOD_HIGH_LOW");
    }

    @Test
    void DIVIDEND_SUMMARY_정상_흐름은_기존_DividendHistoryMapper를_그대로_쓴다() {
        stubIntent("DIVIDEND_SUMMARY", "코카콜라", null);
        stubAnswer("코카콜라는 분기마다 0.48달러씩 배당했습니다.");
        when(stockResolver.resolve("코카콜라", null))
                .thenReturn(new StockResolver.Resolution("KO", "코카콜라", List.of()));
        when(dividendHistoryMapper.findRecentByStockCd(eq("KO"), anyInt()))
                .thenReturn(List.of(DividendHistory.builder()
                        .stockCd("KO").exDate(LocalDate.of(2026, 2, 1)).dividend(new BigDecimal("0.48"))
                        .build()));

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("코카콜라 최근 배당 얼마씩 줬어"));

        assertThat(res.questionType()).isEqualTo("DIVIDEND_SUMMARY");
        verify(dividendHistoryMapper).findRecentByStockCd("KO", 8);
        verifyNoInteractions(stockPriceHistoryMapper);
    }

    // ── 실패 지점 ────────────────────────────────────────────────────────────────

    @Test
    void 질문유형이_UNKNOWN이면_아무것도_조회하지_않고_안내문구로_끝난다() {
        stubIntent("UNKNOWN", null, null);

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("오늘 날씨 어때"));

        assertThat(res.questionType()).isEqualTo("UNKNOWN");
        assertThat(res.answer()).contains("이해하지 못했");
        verifyNoInteractions(stockResolver, stockPriceHistoryMapper, dividendHistoryMapper, marketNewsLookupService);
    }

    @Test
    void 종목을_전혀_못_찾으면_후보_없이_안내한다() {
        stubIntent("MAX_DROP_DAY", "듣보잡종목", "올해");
        when(stockResolver.resolve("듣보잡종목", null))
                .thenReturn(new StockResolver.Resolution(null, null, List.of()));

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("듣보잡종목 올해 가장 하락한 날"));

        assertThat(res.answer()).contains("찾지 못했어요");
        verifyNoInteractions(stockPriceHistoryMapper, marketNewsLookupService);
    }

    @Test
    void 동명이의_종목은_후보를_나열한다() {
        stubIntent("MAX_DROP_DAY", "삼성", "올해");
        when(stockResolver.resolve("삼성", null)).thenReturn(new StockResolver.Resolution(
                null, null, List.of(new StockRef("005930.KS", "삼성전자"), new StockRef("006400.KS", "삼성SDI"))));

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("삼성 올해 가장 하락한 날"));

        assertThat(res.answer()).contains("삼성전자").contains("삼성SDI");
        verifyNoInteractions(stockPriceHistoryMapper);
    }

    @Test
    void 기간_내_가격_이력이_없으면_안내하고_뉴스는_조회하지_않는다() {
        stubIntent("MAX_DROP_DAY", "애플", "올해");
        when(stockResolver.resolve("애플", null))
                .thenReturn(new StockResolver.Resolution("AAPL", "애플", List.of()));
        when(stockPriceHistoryMapper.findMaxChangeDay(eq("AAPL"), any(), any(), eq("DROP")))
                .thenReturn(Optional.empty());

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("올해 애플 가장 하락한 날"));

        assertThat(res.answer()).contains("찾지 못했어요");
        verifyNoInteractions(marketNewsLookupService);
    }

    @Test
    void 뉴스를_못_찾아도_가격_사실_답변은_정상_생성된다() {
        stubIntent("MAX_DROP_DAY", "애플", "올해");
        stubAnswer("2026년 3월 11일 애플은 5% 하락했습니다. 관련 뉴스는 찾지 못했습니다.");
        when(stockResolver.resolve("애플", null))
                .thenReturn(new StockResolver.Resolution("AAPL", "애플", List.of()));
        when(stockPriceHistoryMapper.findMaxChangeDay(eq("AAPL"), any(), any(), eq("DROP")))
                .thenReturn(Optional.of(StockPriceMoveRow.builder()
                        .priceDt(LocalDate.of(2026, 3, 11))
                        .prevClose(new BigDecimal("250")).closePrice(new BigDecimal("237.5"))
                        .changePct(new BigDecimal("-5.0")).build()));
        when(marketNewsLookupService.findNewsForDate(any(), any(), any())).thenReturn(List.of());

        MarketQuestionResponse res = service.ask(new MarketQuestionRequest("올해 애플 가장 하락한 날"));

        assertThat(res.answer()).contains("찾지 못했습니다");
    }

    @Test
    void 의도추출_LLM_호출이_실패하면_서비스이용불가_예외() {
        when(aiGatewayClient.chat(eq(QuestionIntentExtractionPrompt.SYSTEM), anyString()))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.ask(new MarketQuestionRequest("올해 애플 가장 하락한 날")))
                .isInstanceOf(MarketQuestionUnavailableException.class);
    }

    @Test
    void 서술_LLM_호출이_실패하면_서비스이용불가_예외() {
        stubIntent("MAX_DROP_DAY", "애플", "올해");
        when(stockResolver.resolve("애플", null))
                .thenReturn(new StockResolver.Resolution("AAPL", "애플", List.of()));
        when(stockPriceHistoryMapper.findMaxChangeDay(eq("AAPL"), any(), any(), eq("DROP")))
                .thenReturn(Optional.of(StockPriceMoveRow.builder()
                        .priceDt(LocalDate.of(2026, 3, 11))
                        .prevClose(new BigDecimal("250")).closePrice(new BigDecimal("237.5"))
                        .changePct(new BigDecimal("-5.0")).build()));
        when(marketNewsLookupService.findNewsForDate(any(), any(), any())).thenReturn(List.of());
        when(aiGatewayClient.chat(eq(MarketAnswerPrompt.SYSTEM), anyString()))
                .thenThrow(new RuntimeException("ai down"));

        assertThatThrownBy(() -> service.ask(new MarketQuestionRequest("올해 애플 가장 하락한 날")))
                .isInstanceOf(MarketQuestionUnavailableException.class);
    }

    private StockPriceHistory priceRow(String stockCd, LocalDate dt, String close) {
        return StockPriceHistory.builder()
                .stockCd(stockCd).priceDt(dt)
                .closePrice(new BigDecimal(close)).highPrice(new BigDecimal(close)).lowPrice(new BigDecimal(close))
                .build();
    }
}

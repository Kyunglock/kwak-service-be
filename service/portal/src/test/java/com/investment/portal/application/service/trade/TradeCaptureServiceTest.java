package com.investment.portal.application.service.trade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.portal.application.dto.history.transaction.TransactionHistoryAddRequest;
import com.investment.portal.application.dto.history.transaction.TransactionHistoryResponse;
import com.investment.portal.application.dto.trade.*;
import com.investment.portal.application.service.history.TransactionHistoryService;
import com.investment.portal.domain.entity.portfolio.Portfolio;
import com.investment.portal.domain.repository.portfolio.PortfolioMapper;
import com.investment.portal.domain.repository.stock.StockRef;
import kwak.common.ai.AiGatewayClient;
import kwak.common.application.event.ActivityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeCaptureServiceTest {

    private static final String USER = "user-1";
    private static final String OTHER_USER = "user-2";
    private static final Long PORTFOLIO_ID = 10L;
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Mock PortfolioMapper portfolioMapper;
    @Mock AiGatewayClient aiGatewayClient;
    @Mock StockResolver stockResolver;
    @Mock TradeDraftStore draftStore;
    @Mock TransactionHistoryService transactionHistoryService;
    @Mock ApplicationEventPublisher eventPublisher;

    private TradeCaptureServiceImpl service;

    @BeforeEach
    void setUp() {
        // 파서는 실제 구현을 쓴다 — LLM 응답 → 초안 변환까지가 이 기능의 본체라서
        // 여기서 mock으로 끊으면 정작 깨지기 쉬운 경로가 테스트에서 빠진다.
        service = new TradeCaptureServiceImpl(
                portfolioMapper, aiGatewayClient,
                new TradeExtractionParser(new ObjectMapper()),
                stockResolver, draftStore, transactionHistoryService, eventPublisher);

        Portfolio mine = Portfolio.builder()
                .portfolioId(PORTFOLIO_ID).userId(USER).portfolioNm("내 포트폴리오").build();
        when(portfolioMapper.findByPortfolioId(PORTFOLIO_ID)).thenReturn(mine);
    }

    private void aiReturns(String json) {
        when(aiGatewayClient.chat(anyString(), anyString()))
                .thenReturn(new AiGatewayClient.ChatResponse(json, 0, 0));
        when(aiGatewayClient.vision(anyString(), anyString(), anyList()))
                .thenReturn(new AiGatewayClient.ChatResponse(json, 0, 0));
    }

    private void resolves(String stockCd, String stockNm) {
        when(stockResolver.resolve(any(), any()))
                .thenReturn(StockResolverFixtures.resolved(stockCd, stockNm));
    }

    private TradeCaptureTextRequest textRequest(String text) {
        return new TradeCaptureTextRequest(PORTFOLIO_ID, text);
    }

    // ── 소유권 ───────────────────────────────────────────────────────────────────

    @Test
    void 남의_포트폴리오에는_기록할_수_없고_AI도_호출하지_않는다() {
        Portfolio others = Portfolio.builder()
                .portfolioId(99L).userId(OTHER_USER).build();
        when(portfolioMapper.findByPortfolioId(99L)).thenReturn(others);

        assertThatThrownBy(() -> service.captureText(USER, new TradeCaptureTextRequest(99L, "애플 1주")))
                .isInstanceOf(PortfolioAccessDeniedException.class);

        verifyNoInteractions(aiGatewayClient);
    }

    @Test
    void 존재하지_않는_포트폴리오도_거부된다() {
        when(portfolioMapper.findByPortfolioId(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.captureText(USER, new TradeCaptureTextRequest(404L, "애플 1주")))
                .isInstanceOf(PortfolioAccessDeniedException.class);
    }

    // ── 초안 생성 ────────────────────────────────────────────────────────────────

    @Test
    void 정상_추출은_READY로_초안에_담기고_저장은_하지_않는다() {
        aiReturns("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":10,
                            "price":230.15,"date":"%s","currency":"USD"}]}
                """.formatted(TODAY));
        resolves("AAPL", "Apple Inc.");

        TradeDraftResponse draft = service.captureText(USER, textRequest("애플 10주 230.15에 샀어"));

        assertThat(draft.items()).hasSize(1);
        TradeDraftItem item = draft.items().get(0);
        assertThat(item.status()).isEqualTo("READY");
        assertThat(item.stockCd()).isEqualTo("AAPL");
        assertThat(item.qty()).isEqualByComparingTo("10");
        assertThat(draft.readyCount()).isEqualTo(1);
        assertThat(draft.needsReviewCount()).isZero();

        // 초안 단계에서는 절대 저장하지 않는다
        verifyNoInteractions(transactionHistoryService);
        verify(draftStore).save(anyString(), eq(USER), eq(PORTFOLIO_ID));
    }

    @Test
    void 종목을_찾지_못하면_NEEDS_STOCK과_후보가_담긴다() {
        aiReturns("""
                {"trades":[{"name":"삼성","type":"BUY","qty":1,"price":70000,"date":"%s"}]}
                """.formatted(TODAY));
        when(stockResolver.resolve(any(), any())).thenReturn(
                StockResolverFixtures.unresolved(List.of(
                        new StockRef("005930.KS", "삼성전자"),
                        new StockRef("006400.KS", "삼성SDI"))));

        TradeDraftResponse draft = service.captureText(USER, textRequest("삼성 1주"));

        TradeDraftItem item = draft.items().get(0);
        assertThat(item.status()).isEqualTo("NEEDS_STOCK");
        assertThat(item.stockCd()).isNull();
        assertThat(item.candidates()).hasSize(2);
        assertThat(draft.needsReviewCount()).isEqualTo(1);
    }

    @Test
    void 수량을_못_읽으면_NEEDS_INPUT() {
        aiReturns("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":null,"price":230,"date":"%s"}]}
                """.formatted(TODAY));
        resolves("AAPL", "Apple Inc.");

        TradeDraftItem item = service.captureText(USER, textRequest("애플 샀어")).items().get(0);

        assertThat(item.status()).isEqualTo("NEEDS_INPUT");
        assertThat(item.issue()).contains("수량");
    }

    @Test
    void 단가를_못_읽으면_NEEDS_INPUT() {
        aiReturns("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":3,"price":null,"date":"%s"}]}
                """.formatted(TODAY));
        resolves("AAPL", "Apple Inc.");

        TradeDraftItem item = service.captureText(USER, textRequest("애플 3주 샀어")).items().get(0);

        assertThat(item.status()).isEqualTo("NEEDS_INPUT");
        assertThat(item.issue()).contains("단가");
    }

    @Test
    void 날짜가_없으면_말없이_오늘로_채운다() {
        aiReturns("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":3,"price":230,"date":null}]}
                """);
        resolves("AAPL", "Apple Inc.");

        TradeDraftItem item = service.captureText(USER, textRequest("애플 3주 230에 샀어")).items().get(0);

        assertThat(item.status()).isEqualTo("READY");
        assertThat(item.transDt()).isEqualTo(TODAY);
        // 날짜까지 또박또박 적는 사용자는 드물다 — 확인을 요구하지 않는다
        assertThat(item.issue()).isNull();
    }

    @Test
    void 미래_날짜로_읽히면_오늘로_맞춘다() {
        aiReturns("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":3,"price":230,"date":"%s"}]}
                """.formatted(TODAY.plusDays(3)));
        resolves("AAPL", "Apple Inc.");

        TradeDraftItem item = service.captureText(USER, textRequest("애플")).items().get(0);

        assertThat(item.status()).isEqualTo("READY");
        assertThat(item.transDt()).isEqualTo(TODAY);
        assertThat(item.issue()).isNull();
    }

    @Test
    void 과거_날짜는_그대로_쓴다() {
        aiReturns("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":3,"price":230,"date":"%s"}]}
                """.formatted(TODAY.minusDays(5)));
        resolves("AAPL", "Apple Inc.");

        TradeDraftItem item = service.captureText(USER, textRequest("지난주에 애플 샀어")).items().get(0);

        assertThat(item.transDt()).isEqualTo(TODAY.minusDays(5));
        assertThat(item.status()).isEqualTo("READY");
    }

    @Test
    void 통화가_없으면_티커로_추정한다() {
        aiReturns("""
                {"trades":[{"name":"삼성전자","type":"BUY","qty":1,"price":70000,"date":"%s","currency":null}]}
                """.formatted(TODAY));
        resolves("005930.KS", "삼성전자");

        assertThat(service.captureText(USER, textRequest("삼성전자 1주")).items().get(0).currency())
                .isEqualTo("KRW");
    }

    @Test
    void 매매_내역이_없으면_빈_초안과_안내문구를_돌려준다() {
        aiReturns("{\"trades\":[]}");

        TradeDraftResponse draft = service.captureText(USER, textRequest("오늘 날씨 어때?"));

        assertThat(draft.items()).isEmpty();
        assertThat(draft.notice()).contains("찾지 못했습니다");
    }

    @Test
    void AI_호출_실패는_재시도_안내로_바뀐다() {
        when(aiGatewayClient.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> service.captureText(USER, textRequest("애플 1주")))
                .isInstanceOf(TradeCaptureUnavailableException.class);
    }

    // ── 이미지 ───────────────────────────────────────────────────────────────────

    @Test
    void 지원하지_않는_이미지_형식은_거부된다() {
        MockMultipartFile pdf = new MockMultipartFile(
                "image", "a.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.captureImage(USER, PORTFOLIO_ID, pdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는");
        verifyNoInteractions(aiGatewayClient);
    }

    @Test
    void 용량을_넘는_이미지는_거부된다() {
        MockMultipartFile big = new MockMultipartFile(
                "image", "a.png", "image/png", new byte[5 * 1024 * 1024]);

        assertThatThrownBy(() -> service.captureImage(USER, PORTFOLIO_ID, big))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4MB");
        verifyNoInteractions(aiGatewayClient);
    }

    @Test
    void 빈_이미지는_거부된다() {
        MockMultipartFile empty = new MockMultipartFile(
                "image", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.captureImage(USER, PORTFOLIO_ID, empty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이미지는_base64로_비전_호출에_실린다() {
        aiReturns("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":1,"price":230,"date":"%s"}]}
                """.formatted(TODAY));
        resolves("AAPL", "Apple Inc.");
        MockMultipartFile png = new MockMultipartFile(
                "image", "a.png", "image/png", new byte[]{1, 2, 3, 4});

        service.captureImage(USER, PORTFOLIO_ID, png);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiGatewayClient.ImagePart>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiGatewayClient).vision(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).mimeType()).isEqualTo("image/png");
        assertThat(captor.getValue().get(0).base64()).isEqualTo("AQIDBA==");
    }

    // ── 확정 ─────────────────────────────────────────────────────────────────────

    private TradeConfirmRequest.Item confirmItem(String stockCd, String qty, String price, LocalDate date) {
        return new TradeConfirmRequest.Item(
                1, stockCd, "BUY", date, new BigDecimal(qty), new BigDecimal(price), "USD", null);
    }

    private void draftExists(String draftId, String owner, Long portfolioId) {
        when(draftStore.find(draftId))
                .thenReturn(Optional.of(new TradeDraftStore.Draft(owner, portfolioId)));
    }

    private void saveSucceeds(long transId) {
        when(transactionHistoryService.addTransaction(any())).thenReturn(
                new TransactionHistoryResponse(transId, PORTFOLIO_ID, "AAPL", "BUY", TODAY,
                        BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, null, null, "USD", null, null));
    }

    @Test
    void 초안이_만료되면_저장되지_않는다() {
        when(draftStore.find("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(USER,
                new TradeConfirmRequest("gone", List.of(confirmItem("AAPL", "1", "230", TODAY)))))
                .isInstanceOf(TradeDraftNotFoundException.class);

        verifyNoInteractions(transactionHistoryService);
    }

    @Test
    void 남의_초안ID로는_저장할_수_없다() {
        draftExists("draft-1", OTHER_USER, 99L);

        assertThatThrownBy(() -> service.confirm(USER,
                new TradeConfirmRequest("draft-1", List.of(confirmItem("AAPL", "1", "230", TODAY)))))
                .isInstanceOf(TradeDraftNotFoundException.class);

        verifyNoInteractions(transactionHistoryService);
    }

    @Test
    void 저장_대상_포트폴리오는_초안에_기록된_값을_쓴다() {
        draftExists("draft-1", USER, PORTFOLIO_ID);
        resolves("AAPL", "Apple Inc.");
        saveSucceeds(100L);

        service.confirm(USER, new TradeConfirmRequest(
                "draft-1", List.of(confirmItem("AAPL", "2", "230", TODAY))));

        ArgumentCaptor<TransactionHistoryAddRequest> captor =
                ArgumentCaptor.forClass(TransactionHistoryAddRequest.class);
        verify(transactionHistoryService).addTransaction(captor.capture());
        assertThat(captor.getValue().portfolioId()).isEqualTo(PORTFOLIO_ID);
        assertThat(captor.getValue().stockCd()).isEqualTo("AAPL");
        assertThat(captor.getValue().qty()).isEqualByComparingTo("2");
    }

    @Test
    void 확정_시점에_종목을_다시_검증해_미등록_종목은_막는다() {
        draftExists("draft-1", USER, PORTFOLIO_ID);
        when(stockResolver.resolve(any(), any())).thenReturn(StockResolverFixtures.unresolved(List.of()));

        TradeConfirmResponse res = service.confirm(USER, new TradeConfirmRequest(
                "draft-1", List.of(confirmItem("FAKE", "1", "230", TODAY))));

        assertThat(res.savedCount()).isZero();
        assertThat(res.failedCount()).isEqualTo(1);
        assertThat(res.results().get(0).message()).contains("등록되지 않은 종목");
        verifyNoInteractions(transactionHistoryService);
    }

    /** 초안은 더 이상 미래 날짜를 만들지 않지만, 손으로 만든 요청은 여전히 막아야 한다. */
    @Test
    void 확정_시점에도_수량_단가_날짜를_다시_검증한다() {
        draftExists("draft-1", USER, PORTFOLIO_ID);
        resolves("AAPL", "Apple Inc.");

        TradeConfirmResponse res = service.confirm(USER, new TradeConfirmRequest("draft-1", List.of(
                confirmItem("AAPL", "0", "230", TODAY),
                confirmItem("AAPL", "1", "0", TODAY),
                confirmItem("AAPL", "1", "230", TODAY.plusDays(1)),
                confirmItem("AAPL", "1", "230", LocalDate.of(1900, 1, 1)))));

        assertThat(res.savedCount()).isZero();
        assertThat(res.failedCount()).isEqualTo(4);
        assertThat(res.results()).extracting(TradeConfirmResponse.Result::message)
                .anySatisfy(m -> assertThat(m).contains("수량"))
                .anySatisfy(m -> assertThat(m).contains("단가"))
                .anySatisfy(m -> assertThat(m).contains("미래"));
        verifyNoInteractions(transactionHistoryService);
    }

    @Test
    void 한_건이_실패해도_나머지는_저장된다() {
        draftExists("draft-1", USER, PORTFOLIO_ID);
        resolves("AAPL", "Apple Inc.");
        when(transactionHistoryService.addTransaction(any()))
                .thenThrow(new IllegalArgumentException("매도할 종목이 포트폴리오에 없습니다"))
                .thenReturn(new TransactionHistoryResponse(200L, PORTFOLIO_ID, "AAPL", "BUY", TODAY,
                        BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, null, null, "USD", null, null));

        TradeConfirmResponse res = service.confirm(USER, new TradeConfirmRequest("draft-1", List.of(
                confirmItem("AAPL", "1", "230", TODAY),
                confirmItem("AAPL", "2", "240", TODAY))));

        assertThat(res.savedCount()).isEqualTo(1);
        assertThat(res.failedCount()).isEqualTo(1);
        assertThat(res.results().get(0).saved()).isFalse();
        assertThat(res.results().get(1).transId()).isEqualTo(200L);
    }

    @Test
    void 한_건이라도_저장되면_초안은_제거된다() {
        draftExists("draft-1", USER, PORTFOLIO_ID);
        resolves("AAPL", "Apple Inc.");
        saveSucceeds(100L);

        service.confirm(USER, new TradeConfirmRequest(
                "draft-1", List.of(confirmItem("AAPL", "1", "230", TODAY))));

        verify(draftStore).remove("draft-1");

        // ActivityEvent 는 일반 record 라 publishEvent(Object) 오버로드로 발행된다
        ArgumentCaptor<ActivityEvent> event = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().actionType()).isEqualTo("TRADE_CAPTURE_CONFIRM");
        assertThat(event.getValue().userId()).isEqualTo(USER);
    }

    @Test
    void 전부_실패하면_초안을_남겨_다시_시도할_수_있게_한다() {
        draftExists("draft-1", USER, PORTFOLIO_ID);
        when(stockResolver.resolve(any(), any())).thenReturn(StockResolverFixtures.unresolved(List.of()));

        service.confirm(USER, new TradeConfirmRequest(
                "draft-1", List.of(confirmItem("FAKE", "1", "230", TODAY))));

        verify(draftStore, never()).remove(anyString());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 매도도_저장된다() {
        draftExists("draft-1", USER, PORTFOLIO_ID);
        resolves("AAPL", "Apple Inc.");
        saveSucceeds(300L);

        service.confirm(USER, new TradeConfirmRequest("draft-1", List.of(
                new TradeConfirmRequest.Item(1, "AAPL", "sell", TODAY,
                        BigDecimal.ONE, new BigDecimal("230"), "USD", null))));

        ArgumentCaptor<TransactionHistoryAddRequest> captor =
                ArgumentCaptor.forClass(TransactionHistoryAddRequest.class);
        verify(transactionHistoryService).addTransaction(captor.capture());
        assertThat(captor.getValue().transType()).isEqualTo("SELL");
    }

    /** Resolution 은 정적 팩터리가 package-private 이라 테스트 전용 헬퍼로 감싼다. */
    static final class StockResolverFixtures {
        static StockResolver.Resolution resolved(String stockCd, String stockNm) {
            return new StockResolver.Resolution(stockCd, stockNm, List.of());
        }

        static StockResolver.Resolution unresolved(List<StockRef> candidates) {
            return new StockResolver.Resolution(null, null, candidates);
        }
    }
}

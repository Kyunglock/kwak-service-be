package com.investment.portal.application.service.trade;

import com.investment.portal.domain.repository.stock.StockRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 규칙 추출기는 "맞는 것만 빠르게"가 목표다. 그래서 테스트도 두 축이다:
 * 전형적인 문장은 즉시 읽혀야 하고, 조금이라도 애매한 문장은 반드시 비워서 LLM 으로 넘겨야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuleBasedTradeExtractorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Mock StockResolver stockResolver;

    private RuleBasedTradeExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new RuleBasedTradeExtractor(stockResolver, true);
        // 모르는 표기는 전부 미확정
        when(stockResolver.resolve(any(), any()))
                .thenReturn(new StockResolver.Resolution(null, null, List.of()));
    }

    private void knows(String name, String stockCd, String stockNm) {
        when(stockResolver.resolve(eq(name), any()))
                .thenReturn(new StockResolver.Resolution(stockCd, stockNm, List.of()));
    }

    private ExtractedTrade extract(String text) {
        Optional<ExtractedTrade> result = extractor.extract(text, TODAY);
        assertThat(result).as("'%s' 는 규칙으로 읽혀야 한다", text).isPresent();
        return result.get();
    }

    private void fallsBack(String text) {
        assertThat(extractor.extract(text, TODAY)).as("'%s' 는 LLM 으로 넘겨야 한다", text).isEmpty();
    }

    // ── 읽히는 문장 ──────────────────────────────────────────────────────────────

    @Test
    void 국내_매도_문장() {
        knows("삼성전자", "005930.KS", "삼성전자");

        ExtractedTrade t = extract("삼성전자 5주 71,000원에 매도했어");

        assertThat(t.name()).isEqualTo("삼성전자");
        assertThat(t.ticker()).isEqualTo("005930.KS");
        assertThat(t.type()).isEqualTo("SELL");
        assertThat(t.qty()).isEqualByComparingTo("5");
        assertThat(t.price()).isEqualByComparingTo("71000");
        assertThat(t.currency()).isEqualTo("KRW");
        assertThat(t.date()).isNull();
        assertThat(t.amount()).isNull();
    }

    @Test
    void 단가가_뒤에_오는_매수_문장() {
        knows("엔비디아", "NVDA", "NVIDIA Corp");

        ExtractedTrade t = extract("엔비디아 2주 매수, 단가는 180.5");

        assertThat(t.ticker()).isEqualTo("NVDA");
        assertThat(t.type()).isEqualTo("BUY");
        assertThat(t.qty()).isEqualByComparingTo("2");
        assertThat(t.price()).isEqualByComparingTo("180.5");
        // 통화 표기가 없으면 비워 둔다 — 서비스가 티커로 추정한다
        assertThat(t.currency()).isNull();
    }

    @Test
    void 어제_와_달러_표기() {
        knows("애플", "AAPL", "Apple Inc.");

        ExtractedTrade t = extract("어제 애플 10주를 230달러에 샀어");

        assertThat(t.ticker()).isEqualTo("AAPL");
        assertThat(t.type()).isEqualTo("BUY");
        assertThat(t.qty()).isEqualByComparingTo("10");
        assertThat(t.price()).isEqualByComparingTo("230");
        assertThat(t.currency()).isEqualTo("USD");
        assertThat(t.date()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void 달러_기호가_앞에_붙는_티커_입력() {
        knows("AAPL", "AAPL", "Apple Inc.");

        ExtractedTrade t = extract("AAPL 3 shares bought at $230.15");

        assertThat(t.ticker()).isEqualTo("AAPL");
        assertThat(t.type()).isEqualTo("BUY");
        assertThat(t.qty()).isEqualByComparingTo("3");
        assertThat(t.price()).isEqualByComparingTo("230.15");
        assertThat(t.currency()).isEqualTo("USD");
    }

    @Test
    void 조사가_붙은_종목명은_원형으로_못_찾을_때만_떼어본다() {
        knows("카카오", "035720.KS", "카카오");

        ExtractedTrade t = extract("카카오를 3주 45000원에 팔았어");

        assertThat(t.ticker()).isEqualTo("035720.KS");
        assertThat(t.name()).isEqualTo("카카오");
        assertThat(t.type()).isEqualTo("SELL");
    }

    @Test
    void 조사처럼_끝나는_종목명은_원형이_우선이다() {
        // "에코프로"의 '로'를 조사로 보고 떼면 안 된다
        knows("에코프로", "086520.KQ", "에코프로");

        ExtractedTrade t = extract("에코프로 1주 90000원 매수");

        assertThat(t.ticker()).isEqualTo("086520.KQ");
        verify(stockResolver, never()).resolve(eq("에코프"), any());
    }

    @Test
    void 날짜_형식들() {
        knows("애플", "AAPL", "Apple Inc.");

        assertThat(extract("2026-10-20 애플 1주 230 매수").date()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(extract("2026.10.20 애플 1주 230 매수").date()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(extract("2026년 10월 20일 애플 1주 230 매수").date()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(extract("10월 20일 애플 1주 230 매수").date()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(extract("그제 애플 1주 230 매수").date()).isEqualTo(TODAY.minusDays(2));
        assertThat(extract("오늘 애플 1주 230 매수").date()).isEqualTo(TODAY);
    }

    @Test
    void 연도_없는_날짜가_아직_오지_않았으면_작년이다() {
        knows("애플", "AAPL", "Apple Inc.");

        assertThat(extract("12월 20일 애플 1주 230 매수").date()).isEqualTo(LocalDate.of(2025, 12, 20));
    }

    @Test
    void 주당_표기는_수량으로_읽지_않는다() {
        knows("삼성전자", "005930.KS", "삼성전자");

        ExtractedTrade t = extract("삼성전자 5주 1주당 71000원 매도");

        assertThat(t.qty()).isEqualByComparingTo("5");
        assertThat(t.price()).isEqualByComparingTo("71000");
    }

    @Test
    void 띄어쓴_종목명도_붙여서_찾아본다() {
        knows("LG에너지솔루션", "373220.KS", "LG에너지솔루션");

        assertThat(extract("LG 에너지솔루션 2주 400000원 매수").ticker()).isEqualTo("373220.KS");
    }

    // ── LLM 으로 넘기는 문장 ─────────────────────────────────────────────────────

    @Test
    void 매수_매도_표시가_없으면_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        fallsBack("삼성전자 5주 71000원");
    }

    @Test
    void 매수와_매도가_함께_있으면_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        fallsBack("삼성전자 5주 71000원에 샀다가 바로 팔았어");
    }

    @Test
    void 수량이_둘_이상이면_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        knows("SK하이닉스", "000660.KS", "SK하이닉스");
        fallsBack("삼성전자 5주, SK하이닉스 3주 70000원에 매수");
    }

    @Test
    void 숫자가_남으면_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        fallsBack("삼성전자 5주 71000원 72000원 매수");
        fallsBack("3일 전에 삼성전자 5주 71000원 매수");
    }

    @Test
    void 총액_표기는_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        fallsBack("삼성전자 5주 총 355,000원에 매도");
    }

    @Test
    void 한글_단위_금액은_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        fallsBack("삼성전자 5주 7만원에 매도");
    }

    @Test
    void 종목이_확정되지_않으면_넘긴다() {
        when(stockResolver.resolve(eq("삼성"), any())).thenReturn(new StockResolver.Resolution(
                null, null, List.of(new StockRef("005930.KS", "삼성전자"), new StockRef("006400.KS", "삼성SDI"))));
        fallsBack("삼성 5주 71000원 매도");
        fallsBack("없는종목 5주 71000원 매도");
    }

    @Test
    void 종목명_자리에_말이_너무_많으면_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        fallsBack("아까 점심 먹고 나서 생각해보니 삼성전자 5주 71000원에 매도했어");
    }

    @Test
    void 매매_문장이_아니면_넘긴다() {
        fallsBack("오늘 날씨 어때?");
        fallsBack("");
        fallsBack(null);
    }

    @Test
    void 끄면_항상_넘긴다() {
        knows("삼성전자", "005930.KS", "삼성전자");
        RuleBasedTradeExtractor disabled = new RuleBasedTradeExtractor(stockResolver, false);

        assertThat(disabled.extract("삼성전자 5주 71000원 매도", TODAY)).isEmpty();
        verifyNoInteractions(stockResolver);
    }
}

package com.investment.portal.insight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.portal.application.dto.stock.DividendMonthRow;
import com.investment.portal.application.service.insight.DividendInsightBuilder;
import com.investment.portal.application.service.insight.StockInfo;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DividendInsightBuilderTest {

    private final ObjectMapper om = new ObjectMapper();
    private final DividendInsightBuilder builder = new DividendInsightBuilder(om);

    private static PortfolioItem item(String cd, double qty) {
        return PortfolioItem.builder().stockCd(cd).holdQty(BigDecimal.valueOf(qty)).build();
    }

    /** yield 인자는 소수(0.02 = 2%) — StockInfo.dividendYield 규약과 동일 */
    private static StockInfo stock(String cd, double price, double yield, String currency) {
        return new StockInfo(cd, cd, "Tech", "", price, 0.0, 0L, 0.0, yield, price * 0.8, price * 1.2, currency);
    }

    @Test
    void computesNumbersForUsdPortfolio() throws Exception {
        // KO: $80, 수익률 2.5% → 주당 연 $2.0 × 10주 = $20 / AAPL: $300, 수익률 0.5% → $1.5 × 2주 = $3
        // NOPAY: $100, 무배당 × 5주
        List<PortfolioItem> items = List.of(item("KO", 10), item("AAPL", 2), item("NOPAY", 5));
        Map<String, StockInfo> stocks = Map.of(
                "KO", stock("KO", 80, 0.025, "USD"),
                "AAPL", stock("AAPL", 300, 0.005, "USD"),
                "NOPAY", stock("NOPAY", 100, 0.0, "USD"));
        Map<String, Set<Integer>> months = Map.of("KO", Set.of(1, 4, 7, 10), "AAPL", Set.of(2, 5, 8, 11));

        JsonNode j = om.readTree(builder.buildContent(items, stocks, months, "설문 미완료", null));

        // 총 평가 = 800+600+500 = 1900 / 연 배당 = 20+3 = 23 → yield 1.21%
        assertThat(j.get("annualDividendUsd").asDouble()).isEqualTo(23.0);
        assertThat(j.get("annualDividendKrw").asDouble()).isEqualTo(0.0);
        assertThat(j.get("portfolioYield").asDouble()).isCloseTo(1.21, org.assertj.core.data.Offset.offset(0.01));
        // 배당주 비중 = (800+600)/1900 = 73.7%
        assertThat(j.get("dividendStockWeight").asDouble()).isCloseTo(73.7, org.assertj.core.data.Offset.offset(0.1));
        // 월별: 1,4,7,10월 KO / 2,5,8,11월 AAPL → 3·6·9·12월 공백
        assertThat(j.get("monthlyFlow")).hasSize(12);
        assertThat(j.get("monthlyFlow").get(0).asInt()).isEqualTo(1);
        assertThat(j.get("monthlyFlow").get(2).asInt()).isEqualTo(0);
        assertThat(j.get("summary").asText()).contains("3종목").contains("2종목");
        assertThat(j.get("findings").toString()).contains("공백");
        // 설문 미완료 → 대조 불가 안내
        assertThat(j.get("profileContrast").asText()).contains("설문");
    }

    @Test
    void mixedCurrencyUsesFixedRate() throws Exception {
        // 삼성전자: ₩300,000 × 10주 = ₩3,000,000, 수익률 1.0% → 연 ₩30,000
        // KO: $80 × 10주 = $800 → ₩1,200,000 (환율 1500), 연 $20 → ₩30,000
        List<PortfolioItem> items = List.of(item("005930.KS", 10), item("KO", 10));
        Map<String, StockInfo> stocks = Map.of(
                "005930.KS", stock("005930.KS", 300_000, 0.01, "KRW"),
                "KO", stock("KO", 80, 0.025, "USD"));
        JsonNode j = om.readTree(builder.buildContent(items, stocks, Map.of(), "설문 미완료", null));

        assertThat(j.get("annualDividendUsd").asDouble()).isEqualTo(20.0);
        assertThat(j.get("annualDividendKrw").asDouble()).isEqualTo(30_000.0);
        // 총 ₩4,200,000 / 연 ₩60,000 → 1.43%
        assertThat(j.get("portfolioYield").asDouble()).isCloseTo(1.43, org.assertj.core.data.Offset.offset(0.01));
        assertThat(j.get("dividendStockWeight").asDouble()).isEqualTo(100.0);
    }

    @Test
    void emptyPortfolioReturnsGuide() throws Exception {
        JsonNode j = om.readTree(builder.buildContent(List.of(), Map.of(), Map.of(), "설문 미완료", null));
        assertThat(j.get("summary").asText()).contains("종목을 추가");
        assertThat(j.get("annualDividendUsd").asDouble()).isEqualTo(0.0);
        assertThat(j.get("monthlyFlow")).hasSize(12);
    }

    @Test
    void llmNarrationOverridesTemplatesButNotNumbers() throws Exception {
        List<PortfolioItem> items = List.of(item("KO", 10));
        Map<String, StockInfo> stocks = Map.of("KO", stock("KO", 80, 0.025, "USD"));
        String llm = "{\"summary\":\"LLM 총평\",\"profileContrast\":\"LLM 대조\",\"findings\":[\"LLM 발견\"],\"annualDividendUsd\":9999}";

        JsonNode j = om.readTree(builder.buildContent(items, stocks, Map.of(), "설문 미완료", llm));

        assertThat(j.get("summary").asText()).isEqualTo("LLM 총평");
        assertThat(j.get("profileContrast").asText()).isEqualTo("LLM 대조");
        assertThat(j.get("findings").get(0).asText()).isEqualTo("LLM 발견");
        assertThat(j.get("annualDividendUsd").asDouble()).isEqualTo(20.0); // 서버 계산값 유지
    }

    @Test
    void invalidLlmJsonFallsBackToTemplates() throws Exception {
        List<PortfolioItem> items = List.of(item("KO", 10));
        Map<String, StockInfo> stocks = Map.of("KO", stock("KO", 80, 0.025, "USD"));
        JsonNode j = om.readTree(builder.buildContent(items, stocks, Map.of(), "설문 미완료", "JSON 아님"));
        assertThat(j.get("summary").asText()).isNotEmpty().doesNotContain("LLM");
    }

    @Test
    void profileContrastUsesSurveyCode() throws Exception {
        List<PortfolioItem> items = List.of(item("KO", 10));
        Map<String, StockInfo> stocks = Map.of("KO", stock("KO", 80, 0.025, "USD"));
        String survey = "투자 성향 코드: GSL\n수익추구 72 / 리스크허용 38 / 장기투자 65";
        JsonNode j = om.readTree(builder.buildContent(items, stocks, Map.of(), survey, null));
        // 안정(S) 성향 + 배당주 비중 100%(높음) → 일치 서술
        assertThat(j.get("profileContrast").asText()).doesNotContain("설문");
    }

    @Test
    void toMonthsMapGroupsByStock() {
        DividendMonthRow r1 = new DividendMonthRow(); r1.setStockCd("KO"); r1.setDivMonth(1);
        DividendMonthRow r2 = new DividendMonthRow(); r2.setStockCd("KO"); r2.setDivMonth(4);
        Map<String, Set<Integer>> m = DividendInsightBuilder.toMonthsMap(List.of(r1, r2));
        assertThat(m.get("KO")).containsExactly(1, 4);
    }

    @Test
    void promptBlockContainsMetrics() {
        List<PortfolioItem> items = List.of(item("KO", 10));
        Map<String, StockInfo> stocks = Map.of("KO", stock("KO", 80, 0.025, "USD"));
        String block = builder.promptBlock(items, stocks, Map.of("KO", Set.of(1, 4, 7, 10)));
        assertThat(block).contains("배당 컨텍스트").contains("%");
    }
}

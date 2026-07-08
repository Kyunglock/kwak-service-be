package com.investment.portal.insight;

import com.investment.portal.application.service.insight.CombinedInsightPromptBuilder;
import com.investment.portal.application.service.insight.InsightPromptContext;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CombinedInsightPromptBuilderTest {

    private final CombinedInsightPromptBuilder builder = new CombinedInsightPromptBuilder();

    @Test
    void promptContainsContextAndSchemaKeys() {
        InsightPromptContext ctx = new InsightPromptContext(
                3, 2,
                "투자 성향 코드: GSL\n수익추구 72 / 리스크허용 38 / 장기투자 65",
                "평균 PER: 22.1 | 배당수익률: 1.30% | 52주 평균 위치: 61%",
                "삼성전자(005930.KS) | 섹터: Tech | ...",
                "배당 컨텍스트: 배당주 비중 74% | 포트폴리오 배당수익률 1.21% | 월별 지급 종목 수(1~12월) [1,1,0,1,1,0,1,1,0,1,1,0]");
        String prompt = builder.build(ctx);

        assertThat(prompt).contains("삼성전자(005930.KS)");
        assertThat(prompt).contains("GSL");
        assertThat(prompt).contains("평균 PER: 22.1");
        assertThat(prompt).contains("종목 3개").contains("섹터 2개");
        assertThat(prompt).contains("배당 컨텍스트").contains("배당주 비중 74%");
        assertThat(prompt).contains("profile_fit")
                .contains("risk_assessment")
                .contains("portfolio_alignment")
                .contains("investment_recommendation")
                .contains("dividend_insight");
        assertThat(CombinedInsightPromptBuilder.SYSTEM_PROMPT).contains("dividend_insight");
    }

    @Test
    void systemPromptForcesKoreanJson() {
        assertThat(CombinedInsightPromptBuilder.SYSTEM_PROMPT)
                .contains("JSON").contains("한국어");
    }
}

package com.investment.portal.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.portal.application.service.insight.CombinedInsight;
import com.investment.portal.application.service.insight.CombinedInsightParser;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CombinedInsightParserTest {

    private final CombinedInsightParser parser = new CombinedInsightParser(new ObjectMapper());

    @Test
    void parsesFullResponse() {
        String raw = """
            {"profile_fit":{"fit":[{"ticker":"삼성전자","level":"보통","reason":"성장 비중 낮음"}],"rebalance":["배당주 확대"]},
             "risk_assessment":["리스크 문장1","리스크 문장2"],
             "portfolio_alignment":["정합성 문장1"],
             "investment_recommendation":["추천 문장1"]}
            """;
        CombinedInsight c = parser.parse(raw);
        assertThat(c).isNotNull();
        assertThat(c.profileFitJson()).contains("\"fit\"").contains("삼성전자");
        assertThat(c.riskLines()).containsExactly("리스크 문장1", "리스크 문장2");
        assertThat(c.alignmentLines()).containsExactly("정합성 문장1");
        assertThat(c.recommendationLines()).containsExactly("추천 문장1");
        assertThat(c.dividendJson()).isNull();
    }

    @Test
    void missingKeysBecomeEmptyAndNullProfile() {
        CombinedInsight c = parser.parse("{\"risk_assessment\":[\"x\"]}");
        assertThat(c).isNotNull();
        assertThat(c.profileFitJson()).isNull();
        assertThat(c.riskLines()).containsExactly("x");
        assertThat(c.alignmentLines()).isEmpty();
        assertThat(c.recommendationLines()).isEmpty();
        assertThat(c.dividendJson()).isNull();
    }

    @Test
    void returnsNullOnNonJson() {
        assertThat(parser.parse("JSON 아님")).isNull();
        assertThat(parser.parse(null)).isNull();
    }

    @Test
    void parsesDividendInsight() {
        String raw = """
            {"dividend_insight":{"summary":"배당 총평","profileContrast":"대조","findings":["발견1"]},
             "risk_assessment":["x"]}
            """;
        CombinedInsight c = parser.parse(raw);
        assertThat(c).isNotNull();
        assertThat(c.dividendJson()).contains("배당 총평").contains("findings");
    }
}

package com.investment.portal.application.service.qa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsKeywordsTest {

    @Test
    void 법인_접미사를_떼어_기사에_쓰이는_이름으로_만든다() {
        assertThat(NewsKeywords.shortName("Apple Inc.")).isEqualTo("Apple");
        assertThat(NewsKeywords.shortName("Microsoft Corporation")).isEqualTo("Microsoft");
        assertThat(NewsKeywords.shortName("The Goldman Sachs Group, Inc.")).isEqualTo("Goldman Sachs");
        assertThat(NewsKeywords.shortName("Alphabet Inc. Class A")).isEqualTo("Alphabet");
        assertThat(NewsKeywords.shortName("The Coca-Cola Company")).isEqualTo("Coca-Cola");
    }

    @Test
    void 접미사가_아닌_단어는_건드리지_않는다() {
        assertThat(NewsKeywords.shortName("Costco Wholesale")).isEqualTo("Costco Wholesale");
        assertThat(NewsKeywords.shortName("삼성전자")).isEqualTo("삼성전자");
        assertThat(NewsKeywords.shortName("Inc")).isEqualTo("Inc");
    }

    @Test
    void 약식명_티커_한글명_순서로_중복없이_만든다() {
        assertThat(NewsKeywords.build("Apple Inc.", "AAPL", "애플"))
                .containsExactly("Apple", "AAPL", "애플");
        assertThat(NewsKeywords.build("NVIDIA Corporation", "NVDA", null))
                .containsExactly("NVIDIA", "NVDA");
    }

    @Test
    void 국내_종목_코드는_거래소_접미사를_뗀다() {
        assertThat(NewsKeywords.build("삼성전자", "005930.KS", null))
                .containsExactly("삼성전자", "005930");
    }

    @Test
    void 두_글자_이하_영문_키워드는_버리고_한글은_남긴다() {
        assertThat(NewsKeywords.build("Ford Motor Company", "F", "포드"))
                .containsExactly("Ford Motor", "포드");
        assertThat(NewsKeywords.build("기아", "000270.KS", null))
                .containsExactly("기아", "000270");
    }
}

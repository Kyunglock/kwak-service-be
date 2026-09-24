package com.investment.portal.application.service.qa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTypeTest {

    @Test
    void 정확한_값은_그대로_매핑된다() {
        assertThat(QuestionType.fromRaw("MAX_DROP_DAY")).isEqualTo(QuestionType.MAX_DROP_DAY);
        assertThat(QuestionType.fromRaw("DIVIDEND_SUMMARY")).isEqualTo(QuestionType.DIVIDEND_SUMMARY);
    }

    @Test
    void 대소문자와_공백에_방어적이다() {
        assertThat(QuestionType.fromRaw("  max_drop_day  ")).isEqualTo(QuestionType.MAX_DROP_DAY);
    }

    @Test
    void 화이트리스트_밖_값은_UNKNOWN() {
        assertThat(QuestionType.fromRaw("SOMETHING_ELSE")).isEqualTo(QuestionType.UNKNOWN);
        assertThat(QuestionType.fromRaw(null)).isEqualTo(QuestionType.UNKNOWN);
        assertThat(QuestionType.fromRaw("")).isEqualTo(QuestionType.UNKNOWN);
        assertThat(QuestionType.fromRaw("  ")).isEqualTo(QuestionType.UNKNOWN);
    }

    @Test
    void UNKNOWN_자체를_넣어도_UNKNOWN() {
        assertThat(QuestionType.fromRaw("UNKNOWN")).isEqualTo(QuestionType.UNKNOWN);
    }
}

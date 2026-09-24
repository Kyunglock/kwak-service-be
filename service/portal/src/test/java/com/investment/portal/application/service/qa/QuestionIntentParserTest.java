package com.investment.portal.application.service.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionIntentParserTest {

    private final QuestionIntentParser parser = new QuestionIntentParser(new ObjectMapper());

    @Test
    void 정상_JSON은_그대로_추출된다() {
        ExtractedQuestionIntent intent = parser.parse("""
                {"questionType":"MAX_DROP_DAY","stockName":"애플","periodHint":"올해"}
                """);

        assertThat(intent.questionType()).isEqualTo("MAX_DROP_DAY");
        assertThat(intent.stockName()).isEqualTo("애플");
        assertThat(intent.periodHint()).isEqualTo("올해");
    }

    @Test
    void 마크다운_코드블록으로_감싸도_파싱된다() {
        ExtractedQuestionIntent intent = parser.parse("""
                ```json
                {"questionType":"DIVIDEND_SUMMARY","stockName":"코카콜라","periodHint":null}
                ```
                """);

        assertThat(intent.questionType()).isEqualTo("DIVIDEND_SUMMARY");
        assertThat(intent.stockName()).isEqualTo("코카콜라");
        assertThat(intent.periodHint()).isNull();
    }

    @Test
    void 문자열_null과_NA는_null로_취급된다() {
        ExtractedQuestionIntent intent = parser.parse("""
                {"questionType":"PERIOD_RETURN","stockName":"null","periodHint":"N/A"}
                """);

        assertThat(intent.stockName()).isNull();
        assertThat(intent.periodHint()).isNull();
    }

    @Test
    void JSON이_아니면_UNKNOWN으로_흡수된다() {
        assertThat(parser.parse(null).questionType()).isEqualTo("UNKNOWN");
        assertThat(parser.parse("").questionType()).isEqualTo("UNKNOWN");
        assertThat(parser.parse("죄송합니다, 이해하지 못했습니다").questionType()).isEqualTo("UNKNOWN");
        assertThat(parser.parse("{망가진 json").questionType()).isEqualTo("UNKNOWN");
    }

    @Test
    void 화이트리스트_검증_자체는_QuestionType이_한다_파서는_원문만_옮긴다() {
        // 파서 책임은 텍스트 전달까지다. "MADE_UP_TYPE" 처럼 화이트리스트 밖 값도
        // 일단 그대로 옮기고, QuestionType.fromRaw 가 UNKNOWN 으로 걸러낸다.
        ExtractedQuestionIntent intent = parser.parse("""
                {"questionType":"MADE_UP_TYPE","stockName":"애플","periodHint":null}
                """);

        assertThat(intent.questionType()).isEqualTo("MADE_UP_TYPE");
        assertThat(QuestionType.fromRaw(intent.questionType())).isEqualTo(QuestionType.UNKNOWN);
    }
}

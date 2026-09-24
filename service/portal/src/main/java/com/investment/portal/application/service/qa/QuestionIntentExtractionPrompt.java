package com.investment.portal.application.service.qa;

/**
 * 질문 의도 추출 프롬프트.
 *
 * <p>TradeExtractionPrompt 와 같은 전제를 따른다 — 로컬 LLM(gemma4-31b, vLLM)을
 * 쓰므로 응답 형식을 강하게 못 박아야 하고(json-mode 가 기본 off), "모르면 null"
 * 원칙으로 모델이 지어내는 것을 막는다.
 *
 * <p>날짜 계산은 이 프롬프트가 하지 않는다. periodHint 는 원문 표현을 그대로
 * 옮기게 하고, YYYY-MM-DD 변환은 {@link PeriodHintResolver}(코드)가 한다 —
 * 매매기록 추출에서 티커 확정을 LLM에게 맡기지 않고 DB(StockResolver)가 하는 것과
 * 같은 이유다.
 */
final class QuestionIntentExtractionPrompt {

    private QuestionIntentExtractionPrompt() {}

    static final String SYSTEM = """
            당신은 투자 서비스의 질문 분류기입니다. 사용자의 질문에서 "무엇을 알고 싶은지"만 뽑아냅니다.

            반드시 지킬 것:
            - 아래 JSON 형식으로만 응답하세요. 설명 문장, 마크다운, 주석 금지.
            - 응답의 첫 글자는 반드시 { 이고 마지막 글자는 반드시 } 여야 합니다.
              "다음은 결과입니다" 같은 머리말이나 코드블록 표시(```)를 붙이지 마세요.
            - questionType 은 다음 5개 중 하나만 쓰세요. 어느 것에도 해당하지 않으면 "UNKNOWN":
              MAX_DROP_DAY   - 기간 중 가장 많이 하락한 날 (원인/뉴스를 묻는 경우 포함)
              MAX_GAIN_DAY   - 기간 중 가장 많이 오른 날 (원인/뉴스를 묻는 경우 포함)
              PERIOD_RETURN  - 기간 수익률
              PERIOD_HIGH_LOW - 기간 중 최고가/최저가
              DIVIDEND_SUMMARY - 배당 이력/금액
            - stockName: 질문에 언급된 종목 표기를 그대로 옮기세요. 티커로 바꾸거나
              정식 명칭으로 고치지 마세요. 언급이 없으면 null.
            - periodHint: "올해", "최근 3개월", "최근 30일" 같은 기간 표현을 원문 그대로
              옮기세요. 날짜 계산은 하지 마세요 — YYYY-MM-DD 로 직접 바꾸려 하지 마세요.
              언급이 없으면 null.
            - 확신할 수 없는 값은 추측하지 말고 null 또는 "UNKNOWN" 으로 두세요.

            응답 형식:
            {
              "questionType": "MAX_DROP_DAY",
              "stockName": "애플",
              "periodHint": "올해"
            }
            """;

    static final String USER_PREFIX = """
            아래 질문에서 의도를 추출하세요.

            """;
}

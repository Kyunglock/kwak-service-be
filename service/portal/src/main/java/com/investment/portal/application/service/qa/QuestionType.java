package com.investment.portal.application.service.qa;

/**
 * 시황 질의응답이 지원하는 고정 질문 유형. 자유 자연어를 목표로 하지 않는다 —
 * LLM이 이 중 하나를 못 고르면 항상 {@link #UNKNOWN}이다({@link QuestionIntentParser}).
 */
public enum QuestionType {
    /** "올해 애플 가장 많이 하락했던 날" */
    MAX_DROP_DAY,
    /** "삼성전자 최근 3개월 중 제일 오른 날" */
    MAX_GAIN_DAY,
    /** "테슬라 올해 수익률" */
    PERIOD_RETURN,
    /** "엔비디아 올해 최고가/최저가" */
    PERIOD_HIGH_LOW,
    /** "코카콜라 최근 배당 얼마씩" */
    DIVIDEND_SUMMARY,
    /** 위 5개 중 어느 것도 아님 — LLM이 화이트리스트 밖 값을 내놓거나 판단 불가일 때 */
    UNKNOWN;

    /** 대소문자·오타에 방어적으로 매핑. 못 찾으면 UNKNOWN — 예외를 던지지 않는다. */
    public static QuestionType fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return QuestionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}

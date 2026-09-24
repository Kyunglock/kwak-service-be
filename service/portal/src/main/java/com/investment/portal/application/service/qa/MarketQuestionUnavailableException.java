package com.investment.portal.application.service.qa;

/** 서술(답변 생성) LLM 호출이 실패했을 때. 사용자에게는 잠시 후 재시도를 안내한다. */
public class MarketQuestionUnavailableException extends RuntimeException {
    public MarketQuestionUnavailableException(Throwable cause) {
        super("지금은 답변을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.", cause);
    }
}

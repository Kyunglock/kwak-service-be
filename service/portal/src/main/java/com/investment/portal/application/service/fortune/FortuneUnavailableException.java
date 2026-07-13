package com.investment.portal.application.service.fortune;

/** LLM 호출 실패/무응답 → 503 (DB 저장 안 함) */
public class FortuneUnavailableException extends RuntimeException {
    public FortuneUnavailableException() {
        super("점술가가 자리를 비웠습니다. 잠시 후 다시 시도해주세요.");
    }

    public FortuneUnavailableException(Throwable cause) {
        super("점술가가 자리를 비웠습니다. 잠시 후 다시 시도해주세요.", cause);
    }
}

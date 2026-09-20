package com.investment.portal.application.service.trade;

/** AI 추출 호출이 실패했을 때. 사용자에게는 잠시 후 재시도를 안내한다. */
public class TradeCaptureUnavailableException extends RuntimeException {
    public TradeCaptureUnavailableException(Throwable cause) {
        super("지금은 매매 내역을 읽지 못했습니다. 잠시 후 다시 시도해 주세요.", cause);
    }
}

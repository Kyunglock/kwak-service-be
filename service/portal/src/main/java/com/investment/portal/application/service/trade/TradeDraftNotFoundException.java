package com.investment.portal.application.service.trade;

/** 초안이 만료됐거나 다른 사용자의 초안일 때. 두 경우를 구분해 알리지 않는다. */
public class TradeDraftNotFoundException extends RuntimeException {
    public TradeDraftNotFoundException(String draftId) {
        super("초안을 찾을 수 없습니다. 만료되었을 수 있으니 다시 입력해 주세요.");
    }
}

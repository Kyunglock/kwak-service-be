package com.investment.portal.application.service.trade;

import java.util.List;

/**
 * LLM 한 번의 추출 결과 전체.
 *
 * <p>화면 종류(screenType)는 줄 단위가 아니라 요청 단위 성질이라 여기 둔다.
 * 잔고 화면이면 "매입일이 원래 없는 화면"이라는 뜻이라, 날짜가 비어 있는 것을
 * 추출 실패로 볼지 화면 특성으로 볼지 판단하는 근거가 된다.
 */
record TradeExtraction(String screenType, List<ExtractedTrade> trades) {

    static final String EXECUTION = "EXECUTION";
    static final String BALANCE = "BALANCE";
    static final String NONE = "NONE";

    static TradeExtraction empty() {
        return new TradeExtraction(NONE, List.of());
    }

    /** 잔고·보유종목 화면 — 체결 시점이 애초에 적혀 있지 않다. */
    boolean isBalanceScreen() {
        return BALANCE.equalsIgnoreCase(screenType);
    }
}

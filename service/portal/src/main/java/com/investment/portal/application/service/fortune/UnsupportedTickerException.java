package com.investment.portal.application.service.fortune;

/** 형식 위반 또는 미등록 티커 → 404 */
public class UnsupportedTickerException extends RuntimeException {
    public UnsupportedTickerException() {
        super("지원하지 않는 종목입니다.");
    }
}

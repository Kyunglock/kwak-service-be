package com.investment.portal.domain.repository.stock;

/** 종목 해석 결과 한 건 (정식 티커 + 표시용 종목명). */
public record StockRef(String stockCd, String stockNm) {}

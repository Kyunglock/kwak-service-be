package com.investment.portal.application.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 기간 내 등락률 최대/최소일 조회 결과 한 줄.
 * (전일 종가 대비 당일 종가 변동률, LAG 윈도우 함수로 계산)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceMoveRow {
    private LocalDate  priceDt;    // 대상일
    private BigDecimal prevClose;  // 전일 종가
    private BigDecimal closePrice; // 당일 종가
    private BigDecimal changePct;  // 전일 대비 등락률(%)
}

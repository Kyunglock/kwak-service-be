package com.investment.portal.application.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 종목별 최근 1년 배당 합계 (배당수익률 산출용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDividendRow {
    private String     stockCd;
    private BigDecimal trailingDividend;   // 최근 1년 주당 배당 합계
}

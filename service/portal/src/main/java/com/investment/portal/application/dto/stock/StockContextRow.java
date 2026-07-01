package com.investment.portal.application.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 인사이트 종목 컨텍스트 조립용 DB 조회 행.
 * tbl_companies + 최신 종가 + 최근 1년 고저 집계 조인 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockContextRow {
    private String     stockCd;
    private String     companyName;
    private String     sector;
    private BigDecimal openPrice;    // 최신 바 시가
    private BigDecimal closePrice;   // 최신 바 종가 (= 현재가)
    private BigDecimal week52High;   // 최근 1년 최고가
    private BigDecimal week52Low;    // 최근 1년 최저가
}

package com.investment.portal.domain.entity.history.stockPrice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceHistory {
    
    private Long priceId;             // 주가ID
    private String stockCd;           // 종목코드
    private LocalDate priceDt;        // 일자
    private BigDecimal openPrice;     // 시가
    private BigDecimal highPrice;     // 고가
    private BigDecimal lowPrice;      // 저가
    private BigDecimal closePrice;    // 종가
    private Long volume;              // 거래량
    private LocalDateTime regDt;      // 등록일시
}
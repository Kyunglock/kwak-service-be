package com.investment.analyzer.market_analyzer.domain.entity.dividend;

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
public class DividendHistory {
    private Long dividendId;       // 배당ID
    private String stockCd;        // 종목코드
    private LocalDate exDate;      // 배당락일
    private BigDecimal dividend;   // 배당금
    private LocalDate paymentDt;   // 배당 지급일
    private LocalDateTime regDt;   // 등록일시
}

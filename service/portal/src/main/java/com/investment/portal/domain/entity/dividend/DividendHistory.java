package com.investment.portal.domain.entity.dividend;

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
    private Long dividendId;
    private String stockCd;
    private LocalDate exDate;
    private BigDecimal dividend;
    private LocalDate paymentDt;
    private LocalDateTime regDt;
}

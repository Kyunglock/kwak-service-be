package com.investment.analyzer.market_analyzer.application.dto.dividend;

import com.investment.analyzer.market_analyzer.domain.entity.dividend.DividendHistory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class DividendHistoryResponse {
    private String stockCd;
    private LocalDate exDate;
    private BigDecimal dividend;
    private LocalDate paymentDt;

    public static DividendHistoryResponse from(DividendHistory entity) {
        return DividendHistoryResponse.builder()
                .stockCd(entity.getStockCd())
                .exDate(entity.getExDate())
                .dividend(entity.getDividend())
                .paymentDt(entity.getPaymentDt())
                .build();
    }
}

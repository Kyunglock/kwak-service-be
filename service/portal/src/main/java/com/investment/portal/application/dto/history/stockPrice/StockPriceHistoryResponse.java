package com.investment.portal.application.dto.history.stockPrice;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "주가 이력 응답 DTO")
public record StockPriceHistoryResponse(
    
    @Schema(description = "주가ID", example = "1")
    Long priceId,
    
    @Schema(description = "종목코드", example = "AAPL")
    String stockCd,
    
    @Schema(description = "일자", example = "2026-01-13")
    LocalDate priceDt,
    
    @Schema(description = "시가", example = "150.25")
    BigDecimal openPrice,
    
    @Schema(description = "고가", example = "152.80")
    BigDecimal highPrice,
    
    @Schema(description = "저가", example = "149.50")
    BigDecimal lowPrice,
    
    @Schema(description = "종가", example = "151.75")
    BigDecimal closePrice,
    
    @Schema(description = "거래량", example = "85420000")
    Long volume
) { }

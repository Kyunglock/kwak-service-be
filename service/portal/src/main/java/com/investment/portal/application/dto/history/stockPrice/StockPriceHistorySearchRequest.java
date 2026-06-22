package com.investment.portal.application.dto.history.stockPrice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주가 이력 검색 DTO")
public record StockPriceHistorySearchRequest(
    
    @Schema(description = "주가ID", example = "1")
    Long priceId,
    
    @Schema(description = "종목코드", example = "AAPL")
    String stockCd
) {
}

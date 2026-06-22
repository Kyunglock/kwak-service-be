package com.investment.portal.application.dto.portfolio.item;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포트폴리오 종목 검색 요청 DTO")
public record PortfolioItemSearchRequest(
    
    @Schema(description = "포트폴리오ID", example = "10")
    Long portfolioId,
    
    @Schema(description = "종목코드", example = "AAPL")
    String stockCd
) {
}

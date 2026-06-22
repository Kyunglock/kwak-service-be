package com.investment.portal.application.dto.portfolio;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포트폴리오 검색 요청 DTO")
public record PortfolioSearchRequest(
    
    @Schema(description = "포트폴리오ID", example = "1")
    Long portfolioId
) {
}

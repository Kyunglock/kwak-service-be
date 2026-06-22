package com.investment.portal.application.dto.history.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 이력 검색 요청 DTO")
public record TransactionHistorySearchRequest(
    
    @Schema(description = "포트폴리오ID", example = "10")
    Long portfolioId
) {
}

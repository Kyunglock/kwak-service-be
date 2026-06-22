package com.investment.portal.application.dto.portfolio.item;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "포트폴리오 종목 삭제 요청 DTO")
public record PortfolioItemDelRequest(
    
    @Schema(description = "항목ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "항목ID는 필수입니다")
    Long itemId
) {
}

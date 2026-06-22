package com.investment.portal.application.dto.portfolio.item;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "포트폴리오 종목 수정 요청 DTO")
public record PortfolioItemModRequest(

    @Schema(description = "항목ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "항목ID는 필수입니다")
    Long itemId,

    @Schema(description = "보유수량", example = "100.0000")
    @DecimalMin(value = "0.0001", message = "보유수량은 0보다 커야 합니다")
    BigDecimal holdQty,

    @Schema(description = "매수단가", example = "155.50")
    @DecimalMin(value = "0.0001", message = "매수단가는 0보다 커야 합니다")
    BigDecimal buyPrice,

    @Schema(description = "매수일자", example = "2026-01-15")
    LocalDate buyDt,

    @Schema(description = "통화", example = "USD")
    String currency,

    @Schema(description = "메모", example = "추가 매수")
    String memo
) {
}

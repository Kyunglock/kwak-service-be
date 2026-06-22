package com.investment.portal.application.dto.portfolio.item;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "포트폴리오 종목 추가 요청 DTO")
public record PortfolioItemAddRequest(
    
    @Schema(description = "포트폴리오ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "포트폴리오ID는 필수입니다")
    Long portfolioId,
    
    @Schema(description = "종목코드", example = "AAPL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "종목코드는 필수입니다")
    String stockCd,
    
    @Schema(description = "보유수량", example = "50.0000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "보유수량은 필수입니다")
    @DecimalMin(value = "0.0001", message = "보유수량은 0보다 커야 합니다")
    BigDecimal holdQty,
    
    @Schema(description = "매수단가", example = "150.25", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "매수단가는 필수입니다")
    @DecimalMin(value = "0.0001", message = "매수단가는 0보다 커야 합니다")
    BigDecimal buyPrice,
    
    @Schema(description = "매수일자", example = "2026-01-13", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "매수일자는 필수입니다")
    LocalDate buyDt,
    
    @Schema(description = "통화", example = "USD")
    String currency,
    
    @Schema(description = "메모", example = "배당 재투자 계획")
    String memo
) {
}

package com.investment.portal.application.dto.history.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "거래 이력 등록 요청 DTO")
public record TransactionHistoryAddRequest(

    @Schema(description = "포트폴리오ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "포트폴리오ID는 필수입니다")
    Long portfolioId,

    @Schema(description = "종목코드", example = "AAPL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "종목코드는 필수입니다")
    String stockCd,

    @Schema(description = "거래유형", example = "BUY", allowableValues = {"BUY", "SELL"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "거래유형은 필수입니다")
    @Pattern(regexp = "BUY|SELL", message = "거래유형은 BUY 또는 SELL만 가능합니다")
    String transType,

    @Schema(description = "거래일자", example = "2026-01-13", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "거래일자는 필수입니다")
    LocalDate transDt,

    @Schema(description = "수량", example = "10.0000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "수량은 필수입니다")
    @DecimalMin(value = "0.0001", message = "수량은 0보다 커야 합니다")
    BigDecimal qty,

    @Schema(description = "단가", example = "150.25", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "단가는 필수입니다")
    @DecimalMin(value = "0.0001", message = "단가는 0보다 커야 합니다")
    BigDecimal price,

    @Schema(description = "수수료", example = "1.50")
    BigDecimal fee,

    @Schema(description = "세금", example = "0.75")
    BigDecimal tax,

    @Schema(description = "통화", example = "USD")
    String currency,

    @Schema(description = "메모", example = "장기 투자 목적 매수")
    String memo
) {
}

package com.investment.portal.application.dto.history.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "거래 이력 수정 요청 DTO")
public record TransactionHistoryModRequest(

    @Schema(description = "거래ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "거래ID는 필수입니다")
    Long transId,

    @Schema(description = "거래일자", example = "2026-01-15")
    LocalDate transDt,

    @Schema(description = "수량", example = "20.0000")
    @DecimalMin(value = "0.0001", message = "수량은 0보다 커야 합니다")
    BigDecimal qty,

    @Schema(description = "단가", example = "155.50")
    @DecimalMin(value = "0.0001", message = "단가는 0보다 커야 합니다")
    BigDecimal price,

    @Schema(description = "메모", example = "추가 매수")
    String memo
) {
}

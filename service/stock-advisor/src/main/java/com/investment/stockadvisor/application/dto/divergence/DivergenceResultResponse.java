package com.investment.stockadvisor.application.dto.divergence;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "Divergence 탐지 결과 응답", description = "재무 이상 신호 탐지 결과")
public record DivergenceResultResponse(

    @Schema(description = "ID", example = "1")
    Long id,

    @Schema(description = "종목 코드", example = "AAPL")
    String stockCd,

    @Schema(description = "회계 연도", example = "2024")
    Integer fiscalYear,

    @Schema(description = "회계 분기", example = "2")
    Integer fiscalQuarter,

    @Schema(description = "Divergence 유형", example = "ACCRUALS_QUALITY",
            allowableValues = {"ACCRUALS_QUALITY", "REVENUE_FCF_GAP"})
    String divergenceType,

    @Schema(description = "심각도 (0.0 ~ 1.0)", example = "0.4500")
    BigDecimal severity,

    @Schema(description = "탐지 근거 (JSON)")
    String evidence,

    @Schema(description = "탐지 일시")
    LocalDateTime detectedAt,

    @Schema(description = "배치 실행일", example = "2024-01-06")
    LocalDate batchRunDt
) {}

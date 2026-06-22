package com.investment.stockadvisor.application.dto.divergence;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Divergence LLM 해석 결과")
public record DivergenceInterpretationResponse(
        @Schema(description = "종목 코드") String stockCd,
        @Schema(description = "Divergence 유형") String divergenceType,
        @Schema(description = "회계 연도") Integer fiscalYear,
        @Schema(description = "회계 분기") Integer fiscalQuarter,
        @Schema(description = "이상 신호 요약") String summary,
        @Schema(description = "리스크 수준 (HIGH/MEDIUM/LOW)") String riskLevel,
        @Schema(description = "주요 원인") List<String> keyDrivers,
        @Schema(description = "모니터링 포인트") List<String> watchPoints,
        @Schema(description = "캐시 응답 여부") boolean cached
) {}

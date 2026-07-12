package com.investment.stockadvisor.application.dto.survey;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "사용자 선호 섹터 응답 DTO", description = "사용자 포트폴리오 기반 선호 섹터 분석 결과")
public record UserPreferredSectorResponse(

        @Schema(description = "섹터 코드 (영문)", example = "Technology")
        String sector,

        @Schema(description = "섹터명 (한글)", example = "기술")
        String sectorKo,

        @Schema(description = "섹터 비중 (%)", example = "35.50")
        BigDecimal sectorPct
) {}

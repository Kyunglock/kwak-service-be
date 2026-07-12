package com.investment.stockadvisor.application.dto.survey;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "사용자 설문 결과 응답 DTO", description = "사용자 설문 응답 결과 (RISK_PROFILE, MARKET_SENTIMENT)")
public record UserRiskProfileResponse(
        @Schema(description = "설문 설명", example = "투자 위험 성향을 파악하기 위한 설문입니다")
        String description,

        @Schema(description = "선택 답변 점수", example = "3")
        Integer score
) {}

package com.investment.survey.application.dto.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "설문 결과 응답 DTO")
public record SurveyResultResponse(
    @Schema(description = "결과ID") Long resultId,
    @Schema(description = "사용자ID") String userId,
    @Schema(description = "설문ID") Long surveyId,
    @Schema(description = "위험 점수") Integer riskScore,
    @Schema(description = "위험 등급 코드") String riskLevelCode,
    @Schema(description = "추천 내용") String recommendation,
    @Schema(description = "포트폴리오 제안") String portfolioSuggestion,
    @Schema(description = "분석일시") LocalDateTime analyzedAt,
    @Schema(description = "유효기한") LocalDateTime validUntil
) {
}

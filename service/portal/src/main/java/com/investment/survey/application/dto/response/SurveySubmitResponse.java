package com.investment.survey.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "설문 제출 결과 응답 DTO")
public record SurveySubmitResponse(
    @Schema(description = "응답ID") Long responseId,
    @Schema(description = "설문ID") Long surveyId,
    @Schema(description = "사용자ID") String userId,
    @Schema(description = "상태 코드") String statusCode,
    @Schema(description = "총점") Integer totalScore,
    @Schema(description = "위험 성향 코드") String riskProfileCode,
    @Schema(description = "완료일시") LocalDateTime completedAt,
    @Schema(description = "총참여자수") Integer totalParticipants
) {
}

package com.investment.survey.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "설문 제출 결과 응답 DTO")
public record SurveyWithMyResponse(
    @Schema(description = "설문ID") Long surveyId,
    @Schema(description = "응답ID") Long responseId,
    @Schema(description = "설문명") String surveyName,
    @Schema(description = "설문 설명") String description,
    @Schema(description = "설문 유형 코드") String surveyTypeCode,
    @Schema(description = "상태 코드") String statusCode,
    @Schema(description = "완료일시") LocalDateTime completedAt,
    @Schema(description = "총참여자수") Integer totalParticipants,
    @Schema(description = "등록일시") LocalDateTime regDt
) {
}

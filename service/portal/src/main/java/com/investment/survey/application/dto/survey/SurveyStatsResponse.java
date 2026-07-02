package com.investment.survey.application.dto.survey;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "설문 통계 요청 DTO")
public record SurveyStatsResponse (
    @Schema(description = "설문ID") Long surveyId,
    @Schema(description = "응답ID") Long responseId,
    @Schema(description = "설문명") String surveyName,
    @Schema(description = "등록일시") LocalDateTime regDt,
    @Schema(description = "총참여자수") Integer totalParticipants,
    @Schema(description = "상태코드") String statusCode
){    
}

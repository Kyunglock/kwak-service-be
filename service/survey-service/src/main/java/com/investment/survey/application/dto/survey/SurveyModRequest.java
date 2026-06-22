package com.investment.survey.application.dto.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "설문 수정 요청 DTO")
public record SurveyModRequest(

    @Schema(description = "설문ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "설문ID는 필수입니다")
    Long surveyId,

    @Schema(description = "설문명", example = "투자 성향 분석 설문 v2")
    String surveyName,

    @Schema(description = "설문 설명")
    String description,

    @Schema(description = "설문 유형 코드", allowableValues = {"RISK_PROFILE", "GOAL", "EXPERIENCE"})
    String surveyTypeCode
) {
}

package com.investment.survey.application.dto.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "설문 등록 요청 DTO")
public record SurveyAddRequest(

    @Schema(description = "설문명", example = "투자 성향 분석 설문", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "설문명은 필수입니다")
    String surveyName,

    @Schema(description = "설문 설명", example = "투자 위험 성향을 파악하기 위한 설문입니다")
    String description,

    @Schema(description = "설문 유형 코드", example = "RISK_PROFILE", allowableValues = {"RISK_PROFILE", "GOAL", "EXPERIENCE"})
    String surveyTypeCode
) {
}

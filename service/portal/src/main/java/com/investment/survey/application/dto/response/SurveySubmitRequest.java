package com.investment.survey.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "설문 제출 요청 DTO")
public record SurveySubmitRequest(

    @Schema(description = "설문ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "설문ID는 필수입니다")
    Long surveyId,

    @Schema(description = "응답 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "응답 목록은 필수입니다")
    @Valid
    List<AnswerRequest> answers
) {
}

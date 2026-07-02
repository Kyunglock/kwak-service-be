package com.investment.survey.application.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선택지 응답 DTO")
public record OptionResponse(
    @Schema(description = "선택지ID") Long optionId,
    @Schema(description = "문항ID") Long questionId,
    @Schema(description = "선택지 텍스트") String optionText,
    @Schema(description = "선택지 값") String optionValue,
    @Schema(description = "정렬 순서") Integer sortOrder,
    @Schema(description = "점수") Integer score
) {
}

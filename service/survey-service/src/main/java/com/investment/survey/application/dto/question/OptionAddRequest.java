package com.investment.survey.application.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "선택지 등록 요청 DTO")
public record OptionAddRequest(

    @Schema(description = "선택지 텍스트", example = "즉시 전액 매도한다", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "선택지 텍스트는 필수입니다")
    String optionText,

    @Schema(description = "선택지 값", example = "CONSERVATIVE")
    String optionValue,

    @Schema(description = "정렬 순서", example = "1")
    Integer sortOrder,

    @Schema(description = "점수", example = "1")
    Integer score
) {
}

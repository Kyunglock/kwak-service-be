package com.investment.survey.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "개별 문항 응답 요청 DTO")
public record AnswerRequest(

    @Schema(description = "문항ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "문항ID는 필수입니다")
    Long questionId,

    @Schema(description = "선택한 옵션ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "선택한 옵션ID는 필수입니다")
    Long selectedOptionId
) {
}

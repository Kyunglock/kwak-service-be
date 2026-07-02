package com.investment.survey.application.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "설문 문항 등록 요청 DTO")
public record QuestionAddRequest(

    @Schema(description = "문항 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "문항 번호는 필수입니다")
    Integer questionNumber,

    @Schema(description = "문항 내용", example = "투자 시 원금 손실이 발생하면 어떻게 하시겠습니까?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "문항 내용은 필수입니다")
    String questionText,

    @Schema(description = "문항 유형 코드", example = "SINGLE_CHOICE", allowableValues = {"SINGLE_CHOICE", "MULTIPLE_CHOICE", "SCALE"})
    String questionTypeCode,

    @Schema(description = "문항 설명")
    String description,

    @Schema(description = "정렬 순서", example = "1")
    Integer sortOrder,

    @Schema(description = "선택지 목록")
    @Valid
    List<OptionAddRequest> options
) {
}

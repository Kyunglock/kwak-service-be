package com.investment.survey.application.dto.question;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "설문 문항 응답 DTO")
public record QuestionWithOptionStatResponse(
    @Schema(description = "문항ID") Long questionId,
    @Schema(description = "설문ID") Long surveyId,
    @Schema(description = "문항 번호") Integer questionNumber,
    @Schema(description = "문항 내용") String questionText,
    @Schema(description = "문항 유형 코드") String questionTypeCode,
    @Schema(description = "문항 설명") String description,
    @Schema(description = "정렬 순서") Integer sortOrder,
    @Schema(description = "선택옵션ID") Long selectedOptionId,
    @Schema(description = "선택값") String selectedValue,
    @Schema(description = "선택지 목록") List<OptionStatsResponse> options
) {}

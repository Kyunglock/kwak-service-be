package com.investment.survey.application.dto.survey;

import com.investment.survey.application.dto.question.QuestionAddRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "시스템 설문 일괄 생성 요청 DTO (뉴스 크롤러용)")
public record SystemSurveyCreateRequest(

        @Schema(description = "설문명", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "설문명은 필수입니다")
        String surveyName,

        @Schema(description = "설문 설명")
        String description,

        @Schema(description = "설문 유형 코드", example = "DAILY_NEWS")
        String surveyTypeCode,

        @Schema(description = "문항 목록 (선택지 포함)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "문항은 최소 1개 이상이어야 합니다")
        @Valid
        List<QuestionAddRequest> questions
) {
}

package com.investment.survey.application.dto.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Schema(description = "설문 응답 DTO")
@Alias("SurveyResponseDto")
public record SurveyResponse(
    @Schema(description = "설문ID") Long surveyId,
    @Schema(description = "설문명") String surveyName,
    @Schema(description = "설문 설명") String description,
    @Schema(description = "설문 유형 코드") String surveyTypeCode,
    @Schema(description = "등록일시") LocalDateTime regDt,
    @Schema(description = "수정일시") LocalDateTime updDt
) {
}

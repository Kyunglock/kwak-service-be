package com.investment.survey.application.dto.code;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "코드 그룹 등록 요청 DTO")
public record CodeGroupAddRequest(
    @Schema(description = "그룹 코드", example = "SURVEY_TYPE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "그룹 코드는 필수입니다")
    String groupCode,

    @Schema(description = "그룹명", example = "설문 유형", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "그룹명은 필수입니다")
    String groupName,

    @Schema(description = "설명") String description,
    @Schema(description = "정렬 순서", example = "1") Integer sortOrder
) {}

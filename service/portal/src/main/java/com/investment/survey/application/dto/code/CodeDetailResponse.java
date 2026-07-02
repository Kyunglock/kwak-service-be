package com.investment.survey.application.dto.code;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코드 상세 응답 DTO")
public record CodeDetailResponse(
    @Schema(description = "코드 상세 ID") Long codeDetailId,
    @Schema(description = "코드 그룹 ID") Long codeGroupId,
    @Schema(description = "코드값") String codeValue,
    @Schema(description = "코드명") String codeName,
    @Schema(description = "코드 설명") String codeDesc,
    @Schema(description = "정렬 순서") Integer sortOrder
) {}

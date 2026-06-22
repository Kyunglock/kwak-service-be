package com.investment.survey.application.dto.code;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "코드 상세 등록 요청 DTO")
public record CodeDetailAddRequest(
    @Schema(description = "코드 그룹 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "코드 그룹 ID는 필수입니다")
    Long codeGroupId,

    @Schema(description = "코드값", example = "RISK_PROFILE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "코드값은 필수입니다")
    String codeValue,

    @Schema(description = "코드명", example = "위험 성향 분석", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "코드명은 필수입니다")
    String codeName,

    @Schema(description = "코드 설명") String codeDesc,
    @Schema(description = "정렬 순서", example = "1") Integer sortOrder
) {}

package com.investment.survey.application.dto.code;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "코드 그룹 응답 DTO")
public record CodeGroupResponse(
    @Schema(description = "코드 그룹 ID") Long codeGroupId,
    @Schema(description = "그룹 코드") String groupCode,
    @Schema(description = "그룹명") String groupName,
    @Schema(description = "설명") String description,
    @Schema(description = "정렬 순서") Integer sortOrder,
    @Schema(description = "등록일시") LocalDateTime regDt,
    @Schema(description = "수정일시") LocalDateTime updDt,
    @Schema(description = "코드 상세 목록") List<CodeDetailResponse> details
) {}

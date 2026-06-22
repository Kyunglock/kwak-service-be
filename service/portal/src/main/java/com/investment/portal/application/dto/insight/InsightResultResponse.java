package com.investment.portal.application.dto.insight;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "인사이트 결과 응답 DTO (타입별 1건)")
public record InsightResultResponse(

    @Schema(description = "결과 ID")
    Long resultId,

    @Schema(description = "사용자 ID")
    String userId,

    @Schema(description = "인사이트 유형 코드", example = "KEY_FINDINGS",
            allowableValues = {"KEY_FINDINGS", "INVESTMENT_STYLE", "RISK_ASSESSMENT",
                               "PORTFOLIO_ALIGNMENT", "INVESTMENT_RECOMMENDATION"})
    String resultTypeCd,

    @Schema(description = "섹션 제목", example = "주요 발견사항")
    String title,

    @Schema(description = "인사이트 내용")
    String content,

    @Schema(description = "등록일시")
    LocalDateTime regDt,

    @Schema(description = "수정일시")
    LocalDateTime updDt
) {}

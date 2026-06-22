package com.investment.portal.application.dto.portfolio;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "포트폴리오 응답 DTO")
public record PortfolioResponse(
    
    @Schema(description = "포트폴리오ID", example = "1")
    Long portfolioId,
    
    @Schema(description = "사용자ID", example = "user-uuid-1234")
    String userId,
    
    @Schema(description = "포트폴리오명", example = "미국 주식 장기투자")
    String portfolioNm,
    
    @Schema(description = "포트폴리오 설명", example = "기술주 중심의 장기 투자 포트폴리오")
    String portfolioDesc,
    
    @Schema(description = "기준통화", example = "USD")
    String baseCurrency,
    
    @Schema(description = "사용여부", example = "Y")
    String useYn,
    
    @Schema(description = "등록일시", example = "2026-01-13T14:30:00")
    LocalDateTime regDt,
    
    @Schema(description = "수정일시", example = "2026-01-13T15:45:00")
    LocalDateTime updDt
) {
}

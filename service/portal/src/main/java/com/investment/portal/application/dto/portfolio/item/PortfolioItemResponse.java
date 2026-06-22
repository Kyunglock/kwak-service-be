package com.investment.portal.application.dto.portfolio.item;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "포트폴리오 종목 응답 DTO")
public record PortfolioItemResponse(
    
    @Schema(description = "항목ID", example = "1")
    Long itemId,
    
    @Schema(description = "포트폴리오ID", example = "10")
    Long portfolioId,
    
    @Schema(description = "종목코드", example = "AAPL")
    String stockCd,
    
    @Schema(description = "보유수량", example = "50.0000")
    BigDecimal holdQty,
    
    @Schema(description = "매수단가", example = "150.25")
    BigDecimal buyPrice,
    
    @Schema(description = "매수일자", example = "2026-01-13")
    LocalDate buyDt,
    
    @Schema(description = "매수금액", example = "7512.50")
    BigDecimal buyAmount,
    
    @Schema(description = "통화", example = "USD")
    String currency,
    
    @Schema(description = "메모", example = "배당 재투자 계획")
    String memo,
    
    @Schema(description = "사용여부", example = "Y")
    String useYn,
    
    @Schema(description = "등록일시", example = "2026-01-13T14:30:00")
    LocalDateTime regDt,
    
    @Schema(description = "수정일시", example = "2026-01-13T15:45:00")
    LocalDateTime updDt
) {
}

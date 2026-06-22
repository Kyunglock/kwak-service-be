package com.investment.portal.application.dto.history.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "거래 이력 응답 DTO")
public record TransactionHistoryResponse(
    
    @Schema(description = "거래ID", example = "1")
    Long transId,
    
    @Schema(description = "포트폴리오ID", example = "10")
    Long portfolioId,
    
    @Schema(description = "종목코드", example = "AAPL")
    String stockCd,
    
    @Schema(description = "거래유형", example = "BUY", allowableValues = {"BUY", "SELL"})
    String transType,
    
    @Schema(description = "거래일자", example = "2026-01-13")
    LocalDate transDt,
    
    @Schema(description = "수량", example = "10.0000")
    BigDecimal qty,
    
    @Schema(description = "단가", example = "150.25")
    BigDecimal price,
    
    @Schema(description = "거래금액", example = "1502.50")
    BigDecimal amount,
    
    @Schema(description = "수수료", example = "1.50")
    BigDecimal fee,
    
    @Schema(description = "세금", example = "0.75")
    BigDecimal tax,
    
    @Schema(description = "통화", example = "USD")
    String currency,
    
    @Schema(description = "메모", example = "장기 투자 목적 매수")
    String memo,
    
    @Schema(description = "등록일시", example = "2026-01-13T14:30:00")
    LocalDateTime regDt
) {
}

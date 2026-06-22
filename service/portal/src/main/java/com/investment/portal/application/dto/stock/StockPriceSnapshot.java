package com.investment.portal.application.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 실시간 주가 캐시용 스냅샷 DTO
 * DB에 저장하지 않고 인메모리 캐시에서만 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "실시간 주가 스냅샷")
public class StockPriceSnapshot {

    @Schema(description = "종목코드", example = "AAPL")
    private String stockCd;

    @Schema(description = "현재가", example = "151.75")
    private BigDecimal currentPrice;

    @Schema(description = "시가", example = "150.25")
    private BigDecimal openPrice;

    @Schema(description = "고가", example = "152.80")
    private BigDecimal highPrice;

    @Schema(description = "저가", example = "149.50")
    private BigDecimal lowPrice;

    @Schema(description = "거래량", example = "85420000")
    private Long volume;

    @Schema(description = "전일 종가", example = "150.00")
    private BigDecimal previousClose;

    @Schema(description = "변동률(%)", example = "1.17")
    private BigDecimal changePercent;

    @Schema(description = "마지막 업데이트 시각")
    private LocalDateTime updatedAt;
}

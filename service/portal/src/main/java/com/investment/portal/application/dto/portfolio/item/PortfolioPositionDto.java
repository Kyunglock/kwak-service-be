package com.investment.portal.application.dto.portfolio.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포트폴리오 포지션 (종목명·섹터 포함)")
public class PortfolioPositionDto {

    @Schema(description = "항목ID", example = "10")
    private Long itemId;

    @Schema(description = "포트폴리오ID", example = "1")
    private Long portfolioId;

    @Schema(description = "종목코드", example = "AAPL")
    private String stockCd;

    @Schema(description = "종목명", example = "Apple Inc.")
    private String stockNm;

    @Schema(description = "종목명(한글)", example = "애플")
    private String stockNmKo;

    @Schema(description = "섹터(영문)", example = "Technology")
    private String sector;

    @Schema(description = "섹터(한글)", example = "기술")
    private String sectorKo;

    @Schema(description = "보유수량", example = "10.0000")
    private BigDecimal holdQty;

    @Schema(description = "매수단가", example = "150.00")
    private BigDecimal buyPrice;

    @Schema(description = "매수일자", example = "2024-01-15")
    private LocalDate buyDt;

    @Schema(description = "매수금액", example = "1500.00")
    private BigDecimal buyAmount;

    @Schema(description = "통화", example = "USD")
    private String currency;

    @Schema(description = "메모")
    private String memo;

    @Schema(description = "최근 종가 (시세 이력이 없으면 null)", example = "175.30")
    private BigDecimal closePrice;

    @Schema(description = "종가 기준일", example = "2026-07-07")
    private LocalDate priceDt;
}

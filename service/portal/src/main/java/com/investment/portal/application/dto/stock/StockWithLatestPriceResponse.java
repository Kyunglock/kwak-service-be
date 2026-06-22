package com.investment.portal.application.dto.stock;

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
@Schema(description = "기업 정보 + 최근 종가")
public class StockWithLatestPriceResponse {

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

    @Schema(description = "가격 기준일", example = "2024-01-15")
    private LocalDate priceDt;

    @Schema(description = "시가", example = "150.25")
    private BigDecimal openPrice;

    @Schema(description = "고가", example = "152.80")
    private BigDecimal highPrice;

    @Schema(description = "저가", example = "149.50")
    private BigDecimal lowPrice;

    @Schema(description = "종가", example = "151.75")
    private BigDecimal closePrice;

    @Schema(description = "거래량", example = "85420000")
    private Long volume;
}

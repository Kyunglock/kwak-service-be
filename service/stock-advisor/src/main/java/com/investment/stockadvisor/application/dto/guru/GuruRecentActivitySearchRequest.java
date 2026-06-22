package com.investment.stockadvisor.application.dto.guru;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "구루 최근 매매 활동 검색 요청 DTO", description = "구루 최근 매매 활동 검색 조건")
public record GuruRecentActivitySearchRequest(

    @Schema(description = "투자자 고유 코드", example = "WARREN_BUFFETT")
    String guruCd,

    @Schema(description = "매매 유형 (BUY / SELL / ADD / TRIM)", example = "BUY")
    String activityType,

    @Schema(description = "종목 티커 심볼", example = "AAPL")
    String ticker,

    @Schema(description = "매매 발생 분기", example = "Q1 2024")
    String activityDate
) {}

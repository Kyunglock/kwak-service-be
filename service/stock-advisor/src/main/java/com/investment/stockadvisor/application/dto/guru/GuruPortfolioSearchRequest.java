package com.investment.stockadvisor.application.dto.guru;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "구루 포트폴리오 검색 요청 DTO", description = "구루 포트폴리오 보유 현황 검색 조건")
public record GuruPortfolioSearchRequest(

    @Schema(description = "투자자 이름 (부분 검색 가능)", example = "Warren Buffett")
    String investorNm,

    @Schema(description = "종목 티커 심볼", example = "AAPL")
    String ticker,

    @Schema(description = "보고 기준일", example = "Q1 2024")
    String reportDate,

    @Schema(description = "매매 활동 연도", example = "2025")
    Integer activityYear,

    @Schema(description = "매매 활동 분기", example = "4")
    Integer activityQtr
) {}

package com.investment.stockadvisor.application.dto.guru;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "구루 포트폴리오 응답 DTO", description = "구루 포트폴리오 보유 현황 응답")
public record GuruPortfolioResponse(

    @Schema(description = "고유 식별자", example = "1")
    Long id,

    @Schema(description = "투자자 영문명", example = "Warren Buffett")
    String investorNm,

    @Schema(description = "투자자 한글명", example = "워런 버핏")
    String investorKoNm,

    @Schema(description = "투자자 닉네임", example = "오마하의 현인")
    String investorNickname,

    @Schema(description = "투자 철학", example = "가치투자, 장기보유 중심")
    String investPhilosophy,

    @Schema(description = "투자 대가의 명언", example = "Be fearful when others are greedy.")
    String famousQuote,

    @Schema(description = "보유 종목 회사명", example = "Apple Inc.")
    String issuerNm,

    @Schema(description = "종목 티커 심볼", example = "AAPL")
    String ticker,

    @Schema(description = "포트폴리오 내 보유 순위", example = "1")
    Integer rankNo,

    @Schema(description = "포트폴리오 내 비중 (%)", example = "40.50")
    BigDecimal portfolioPct,

    @Schema(description = "보고 기준일", example = "Q1 2024")
    String reportDate,

    @Schema(description = "최근 매매 활동 일자", example = "2025-01-15")
    String activityDate,

    @Schema(description = "매매 활동 연도", example = "2025")
    Integer activityYear,

    @Schema(description = "매매 활동 분기", example = "4")
    Integer activityQtr,

    @Schema(description = "보유 비중 변동률 (%)", example = "1.23")
    BigDecimal changePct,

    @Schema(description = "데이터 등록 일시", example = "2024-01-06T15:30:00")
    LocalDateTime regDt
) {}

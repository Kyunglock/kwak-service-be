package com.investment.stockadvisor.application.dto.guru;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "구루 최근 매매 활동 응답 DTO", description = "구루 최근 매매 활동 응답")
public record GuruRecentActivityResponse(

    @Schema(description = "고유 식별자", example = "1")
    Long activityId,

    @Schema(description = "투자자 고유 코드", example = "WARREN_BUFFETT")
    String guruCd,

    @Schema(description = "투자자 영문명", example = "Warren Buffett")
    String investorNm,

    @Schema(description = "투자자 한글명", example = "워런 버핏")
    String investorKoNm,

    @Schema(description = "투자자 닉네임", example = "오마하의 현인")
    String investorNickname,

    @Schema(description = "매매 유형 (BUY / SELL / ADD / TRIM)", example = "BUY")
    String activityType,

    @Schema(description = "거래 종목 회사명", example = "Apple Inc.")
    String issuerNm,

    @Schema(description = "종목 티커 심볼", example = "AAPL")
    String ticker,

    @Schema(description = "매매 발생 분기", example = "Q1 2024")
    String activityDate,

    @Schema(description = "매매 발생 연도", example = "2025")
    Short activityYear,

    @Schema(description = "매매 발생 분기 (1~4)", example = "1")
    Byte activityQtr,

    @Schema(description = "포지션 변동 비율 (%)", example = "25.50")
    BigDecimal changePct,

    @Schema(description = "비고 및 추가 메모", example = "실적 개선 기대로 신규 매수")
    String notes,

    @Schema(description = "데이터 등록 일시", example = "2024-01-06T15:30:00")
    LocalDateTime regDt
) {}

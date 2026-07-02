package com.investment.analyzer.market_analyzer.application.dto.market;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Schema(name = "시장 통계 응답 DTO", description = "시장 데이터 응답")
public record MarketStatisticsResponse(
    
    @Schema(description = "ID", example = "1")
    Long id,
    
    @Schema(description = "통계 날짜", example = "2024-01-06")
    LocalDate statDate,
    
    @Schema(description = "통계 시간", example = "15:30:00")
    LocalTime statTime,
    
    @Schema(description = "시장 이름", example = "S&P 500")
    String marketName,
    
    @Schema(description = "시장 상황 설명", example = "연준 금리 동결 결정에 시장 혼조세")
    String description,
    
    @Schema(description = "지수 값", example = "4783.45")
    Double indexValue,
    
    @Schema(description = "변화율 (%)", example = "1.25")
    Double changePercentage,
    
    @Schema(description = "변화 방향", example = "UP", allowableValues = {"UP", "DOWN", "FLAT"})
    String changeDirection,
    
    @Schema(description = "생성 일시", example = "2024-01-06T15:30:00")
    LocalDateTime createdAt,
    
    @Schema(description = "수정 일시", example = "2024-01-06T15:30:00")
    LocalDateTime updatedAt
) {}
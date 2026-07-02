package com.investment.analyzer.market_analyzer.domain.entity.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketStatistics {
    
    /**
     * 시장 통계 고유 ID
     */
    private Long id;
    
    /**
     * 통계 기준 날짜
     */
    private LocalDate statDate;
    
    /**
     * 통계 기준 시간
     */
    private LocalTime statTime;
    
    /**
     * 시장명 (예: S&P 500, KOSPI, NASDAQ)
     */
    private String marketName;
    
    /**
     * 시장 상황 설명
     */
    private String description;
    
    /**
     * 지수 값
     */
    private BigDecimal indexValue;
    
    /**
     * 변동률 (%)
     */
    private BigDecimal changePercentage;
    
    /**
     * 변동 방향 (UP, DOWN, NEUTRAL)
     */
    private String changeDirection;
    
    /**
     * 레코드 생성 시각
     */
    private LocalDateTime createdAt;
    
    /**
     * 레코드 수정 시각
     */
    private LocalDateTime updatedAt;
}
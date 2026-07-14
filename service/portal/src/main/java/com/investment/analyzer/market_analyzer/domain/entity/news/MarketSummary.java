package com.investment.analyzer.market_analyzer.domain.entity.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSummary {
    private LocalDate summaryDt;   // 시황 기준일 (KST)
    private String summary;        // LLM 시황 요약 (한국어 3~5문장)
    private Integer articleCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.investment.analyzer.market_analyzer.application.dto.market;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "시장 검색용 DTO", description = "시장 데이터 검색 요청")
public record MarketStatisticsSearchRequest(
    
    @Schema(description = "시장 상황 설명 (부분 검색 가능)", example = "연준 금리 동결 결정에 시장 혼조세")
    String description,
    
    @Schema(description = "시장 이름 (부분 검색 가능)", example = "NASDAQ")
    String marketName
) {}
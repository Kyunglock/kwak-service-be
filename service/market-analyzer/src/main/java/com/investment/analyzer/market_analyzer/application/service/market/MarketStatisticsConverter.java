package com.investment.analyzer.market_analyzer.application.service.market;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.investment.analyzer.market_analyzer.application.dto.market.MarketStatisticsResponse;
import com.investment.analyzer.market_analyzer.domain.entity.market.MarketStatistics;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MarketStatisticsConverter {
    /**
     * Entity -> Response DTO 변환
     */
    MarketStatisticsResponse toResponse(MarketStatistics entity);
    
    /**
     * Entity List -> Response DTO List 변환
     */
    List<MarketStatisticsResponse> toResponseList(List<MarketStatistics> entities);
}

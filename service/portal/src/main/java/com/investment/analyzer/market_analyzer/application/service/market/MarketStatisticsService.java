package com.investment.analyzer.market_analyzer.application.service.market;

import org.springframework.data.domain.Pageable;

import com.investment.analyzer.market_analyzer.application.dto.market.MarketStatisticsResponse;
import com.investment.analyzer.market_analyzer.application.dto.market.MarketStatisticsSearchRequest;

import kwak.common.application.dto.PageResponse;

public interface MarketStatisticsService {
    public PageResponse<MarketStatisticsResponse> findAllMarket(MarketStatisticsSearchRequest searchRequest, Pageable pageable);
}

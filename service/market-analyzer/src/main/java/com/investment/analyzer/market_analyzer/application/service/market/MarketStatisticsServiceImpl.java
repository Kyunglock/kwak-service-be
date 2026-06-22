package com.investment.analyzer.market_analyzer.application.service.market;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.investment.analyzer.market_analyzer.application.dto.market.MarketStatisticsResponse;
import com.investment.analyzer.market_analyzer.application.dto.market.MarketStatisticsSearchRequest;
import com.investment.analyzer.market_analyzer.domain.entity.market.MarketStatistics;
import com.investment.analyzer.market_analyzer.domain.repository.market.MarketStatisticsMapper;

import kwak.common.application.dto.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketStatisticsServiceImpl implements MarketStatisticsService{

    private final MarketStatisticsMapper marketStatisticsMapper;

    private final MarketStatisticsConverter marketStatisticsConverter;

    @Override
    public PageResponse<MarketStatisticsResponse> findAllMarket(MarketStatisticsSearchRequest searchRequest, Pageable pageable) {

        List<MarketStatistics> marketStatisticsList = marketStatisticsMapper.findAllMarket(searchRequest, pageable);

        List<MarketStatisticsResponse> content = marketStatisticsConverter.toResponseList(marketStatisticsList);
        
        int totalElements = marketStatisticsMapper.findAllMarketCnt(searchRequest);

        return PageResponse.of(content, pageable.getPageNumber() + 1, pageable.getPageSize(), totalElements);
    }
}

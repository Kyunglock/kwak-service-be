package com.investment.analyzer.market_analyzer.domain.repository.market;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.domain.Pageable;

import com.investment.analyzer.market_analyzer.application.dto.market.MarketStatisticsSearchRequest;
import com.investment.analyzer.market_analyzer.domain.entity.market.MarketStatistics;

@Mapper
public interface MarketStatisticsMapper {
    List<MarketStatistics> findAllMarket(MarketStatisticsSearchRequest searchRequest, Pageable pageable);

    int findAllMarketCnt(MarketStatisticsSearchRequest searchRequest);
}

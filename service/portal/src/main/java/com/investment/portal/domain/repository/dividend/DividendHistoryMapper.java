package com.investment.portal.domain.repository.dividend;

import com.investment.portal.application.dto.stock.StockDividendRow;
import com.investment.portal.domain.entity.dividend.DividendHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DividendHistoryMapper {

    List<DividendHistory> findRecentBatchByPortfolioId(@Param("portfolioId") Long portfolioId, @Param("limit") int limit);

    /**
     * 종목별 최근 1년 주당 배당 합계 (배당수익률 산출용).
     */
    List<StockDividendRow> findTrailingDividendByStockCodes(@Param("stockCodes") List<String> stockCodes);
}

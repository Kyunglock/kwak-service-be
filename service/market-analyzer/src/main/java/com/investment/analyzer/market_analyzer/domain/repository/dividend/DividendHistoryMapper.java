package com.investment.analyzer.market_analyzer.domain.repository.dividend;

import com.investment.analyzer.market_analyzer.domain.entity.dividend.DividendHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DividendHistoryMapper {

    List<DividendHistory> findByStockCd(@Param("stockCd") String stockCd);

    List<DividendHistory> findRecentByStockCd(@Param("stockCd") String stockCd, @Param("limit") int limit);

    List<DividendHistory> findRecentBatchByStockCds(@Param("stockCds") List<String> stockCds, @Param("limit") int limit);
}

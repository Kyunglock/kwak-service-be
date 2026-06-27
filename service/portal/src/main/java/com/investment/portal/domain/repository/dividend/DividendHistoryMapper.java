package com.investment.portal.domain.repository.dividend;

import com.investment.portal.domain.entity.dividend.DividendHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DividendHistoryMapper {

    List<DividendHistory> findRecentBatchByPortfolioId(@Param("portfolioId") Long portfolioId, @Param("limit") int limit);
}

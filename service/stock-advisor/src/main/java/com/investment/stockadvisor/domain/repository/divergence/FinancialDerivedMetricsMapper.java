package com.investment.stockadvisor.domain.repository.divergence;

import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FinancialDerivedMetricsMapper {

    void upsert(FinancialDerivedMetrics metrics);

    List<FinancialDerivedMetrics> findRecentByStockCd(@Param("stockCd") String stockCd,
                                                       @Param("limit") int limit);
}

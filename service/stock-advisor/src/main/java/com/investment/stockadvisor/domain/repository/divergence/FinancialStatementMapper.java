package com.investment.stockadvisor.domain.repository.divergence;

import com.investment.stockadvisor.domain.entity.divergence.FinancialStatement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FinancialStatementMapper {

    List<FinancialStatement> findRecentByStockCd(@Param("stockCd") String stockCd,
                                                  @Param("limit") int limit);

    List<String> findAllStockCds();
}

package com.investment.stockadvisor.domain.repository.divergence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockSectorMapper {
    String findSectorCodeByStockCd(@Param("stockCd") String stockCd);
    List<String> findStockCdsBySectorCode(@Param("sectorCode") String sectorCode);
}

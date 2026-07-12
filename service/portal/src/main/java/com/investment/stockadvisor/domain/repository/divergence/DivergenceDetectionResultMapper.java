package com.investment.stockadvisor.domain.repository.divergence;

import com.investment.stockadvisor.domain.entity.divergence.DivergenceDetectionResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DivergenceDetectionResultMapper {

    void insert(DivergenceDetectionResult result);

    List<DivergenceDetectionResult> findByStockCd(@Param("stockCd") String stockCd);

    List<DivergenceDetectionResult> findByBatchRunDt(@Param("batchRunDt") LocalDate batchRunDt);
}

package com.investment.stockadvisor.application.service.divergence;

import com.investment.stockadvisor.application.dto.divergence.DivergenceResultResponse;

import java.time.LocalDate;
import java.util.List;

public interface DivergenceDetectionService {

    /** 전체 종목 탐지 실행 및 저장 */
    void detectAndSaveAll();

    /** 특정 종목 탐지 실행 및 저장 */
    void detectAndSave(String stockCd);

    List<DivergenceResultResponse> findByStockCd(String stockCd);

    List<DivergenceResultResponse> findByBatchRunDt(LocalDate batchRunDt);
}

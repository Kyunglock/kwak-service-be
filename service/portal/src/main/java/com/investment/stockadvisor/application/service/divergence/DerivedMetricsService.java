package com.investment.stockadvisor.application.service.divergence;

public interface DerivedMetricsService {

    /** 전체 종목 파생 지표 계산 및 저장 */
    void computeAndSaveAll();

    /** 특정 종목 파생 지표 계산 및 저장 */
    void computeAndSave(String stockCd);
}

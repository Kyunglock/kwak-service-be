package com.investment.stockadvisor.domain.detector;

import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;

import java.util.List;
import java.util.Optional;

/**
 * Divergence 탐지 전략 인터페이스.
 * 각 구현체는 Spring Bean으로 등록되며 DivergenceDetectionServiceImpl에 자동 주입된다.
 */
public interface DivergenceDetector {

    /** 탐지 유형 식별자 (예: ACCRUALS_QUALITY) */
    String getType();

    /**
     * 분기별 시계열를 받아 이상 신호를 탐지한다.
     * 탐지된 경우 DetectionResult, 조건 미충족 시 empty를 반환한다.
     */
    Optional<DetectionResult> detect(List<FinancialDerivedMetrics> timeSeries);
}

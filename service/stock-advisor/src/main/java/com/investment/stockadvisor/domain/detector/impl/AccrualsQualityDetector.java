package com.investment.stockadvisor.domain.detector.impl;

import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.DivergenceDetector;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Accruals Quality Detector
 *
 * 조건: ni_ocf_gap > 0.10 이 2분기 연속
 * 의미: 회계 이익과 현금 흐름의 괴리 누적 → 회계 품질 의심
 * Severity: 0.10 → 0.0, 0.30+ → 1.0 (Phase 2에서 섹터 percentile로 교체)
 */
@Component
public class AccrualsQualityDetector implements DivergenceDetector {

    private static final String TYPE = "ACCRUALS_QUALITY";
    private static final BigDecimal THRESHOLD = new BigDecimal("0.10");
    private static final BigDecimal SEVERITY_RANGE = new BigDecimal("0.20");

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Optional<DetectionResult> detect(List<FinancialDerivedMetrics> timeSeries) {
        if (timeSeries.size() < 2) return Optional.empty();

        List<FinancialDerivedMetrics> sorted = timeSeries.stream()
            .sorted(Comparator.comparing(FinancialDerivedMetrics::getFiscalYear)
                .thenComparing(FinancialDerivedMetrics::getFiscalQuarter))
            .toList();

        FinancialDerivedMetrics latest = sorted.get(sorted.size() - 1);
        FinancialDerivedMetrics prev = sorted.get(sorted.size() - 2);

        if (latest.getNiOcfGap() == null || prev.getNiOcfGap() == null) return Optional.empty();

        if (latest.getNiOcfGap().compareTo(THRESHOLD) <= 0
                || prev.getNiOcfGap().compareTo(THRESHOLD) <= 0) return Optional.empty();

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("niOcfGap_current", latest.getNiOcfGap());
        evidence.put("niOcfGap_prev", prev.getNiOcfGap());
        evidence.put("threshold", THRESHOLD);
        evidence.put("consecutive_quarters", 2);

        return Optional.of(DetectionResult.builder()
            .type(TYPE)
            .severity(computeSeverity(latest.getNiOcfGap()))
            .evidence(evidence)
            .fiscalYear(latest.getFiscalYear())
            .fiscalQuarter(latest.getFiscalQuarter())
            .build());
    }

    private BigDecimal computeSeverity(BigDecimal value) {
        return value.subtract(THRESHOLD)
            .divide(SEVERITY_RANGE, 4, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE)
            .max(BigDecimal.ZERO);
    }
}

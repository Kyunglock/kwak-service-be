package com.investment.stockadvisor.domain.detector.impl;

import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.DivergenceDetector;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Revenue-FCF Gap Detector
 *
 * 조건: (revenue_yoy - fcf_yoy) > 0.20 이 2분기 연속
 * 의미: 매출은 성장하나 FCF는 다라짴 → 운전자본 이슈 또는 수익성 악화 신호
 * Severity: 0.20 → 0.0, 0.60+ → 1.0 (Phase 2에서 섹터 percentile로 교체)
 */
@Component
public class RevenueFcfGapDetector implements DivergenceDetector {

    private static final String TYPE = "REVENUE_FCF_GAP";
    private static final BigDecimal THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal SEVERITY_RANGE = new BigDecimal("0.40");

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

        if (latest.getRevenueYoy() == null || latest.getFcfYoy() == null) return Optional.empty();
        if (prev.getRevenueYoy() == null || prev.getFcfYoy() == null) return Optional.empty();

        BigDecimal latestGap = latest.getRevenueYoy().subtract(latest.getFcfYoy());
        BigDecimal prevGap = prev.getRevenueYoy().subtract(prev.getFcfYoy());

        if (latestGap.compareTo(THRESHOLD) <= 0 || prevGap.compareTo(THRESHOLD) <= 0) return Optional.empty();

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("revenueYoy_current", latest.getRevenueYoy());
        evidence.put("fcfYoy_current", latest.getFcfYoy());
        evidence.put("gap_current", latestGap);
        evidence.put("revenueYoy_prev", prev.getRevenueYoy());
        evidence.put("fcfYoy_prev", prev.getFcfYoy());
        evidence.put("gap_prev", prevGap);
        evidence.put("threshold", THRESHOLD);
        evidence.put("consecutive_quarters", 2);

        return Optional.of(DetectionResult.builder()
            .type(TYPE)
            .severity(computeSeverity(latestGap))
            .evidence(evidence)
            .fiscalYear(latest.getFiscalYear())
            .fiscalQuarter(latest.getFiscalQuarter())
            .build());
    }

    private BigDecimal computeSeverity(BigDecimal gap) {
        return gap.subtract(THRESHOLD)
            .divide(SEVERITY_RANGE, 4, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE)
            .max(BigDecimal.ZERO);
    }
}

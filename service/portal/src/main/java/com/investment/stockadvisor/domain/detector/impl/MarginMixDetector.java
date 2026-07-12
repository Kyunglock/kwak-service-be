package com.investment.stockadvisor.domain.detector.impl;

import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.DivergenceDetector;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MarginMixDetector implements DivergenceDetector {

    private static final String TYPE = "MARGIN_MIX";
    private static final BigDecimal DIVERGENCE_THRESHOLD = new BigDecimal("0.05");
    private static final BigDecimal SEVERITY_RANGE = new BigDecimal("0.15");

    @Override
    public String getType() { return TYPE; }

    @Override
    public Optional<DetectionResult> detect(List<FinancialDerivedMetrics> timeSeries) {
        if (timeSeries.size() < 3) return Optional.empty();

        List<FinancialDerivedMetrics> sorted = timeSeries.stream()
                .sorted(Comparator.comparing(FinancialDerivedMetrics::getFiscalYear)
                        .thenComparing(FinancialDerivedMetrics::getFiscalQuarter))
                .toList();

        int consecutiveCount = 0;
        FinancialDerivedMetrics latest = null;
        BigDecimal maxDivergence = BigDecimal.ZERO;

        for (int i = 1; i < sorted.size(); i++) {
            FinancialDerivedMetrics prev = sorted.get(i - 1);
            FinancialDerivedMetrics curr = sorted.get(i);
            BigDecimal divergence = computeDivergence(prev, curr);

            if (divergence != null && divergence.compareTo(DIVERGENCE_THRESHOLD) > 0) {
                consecutiveCount++;
                latest = curr;
                if (divergence.compareTo(maxDivergence) > 0) maxDivergence = divergence;
            } else {
                consecutiveCount = 0;
            }

            if (consecutiveCount >= 2) break;
        }

        if (consecutiveCount < 2 || latest == null) return Optional.empty();

        BigDecimal severity = maxDivergence.subtract(DIVERGENCE_THRESHOLD)
                .divide(SEVERITY_RANGE, 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);

        return Optional.of(DetectionResult.builder()
                .type(TYPE)
                .severity(severity)
                .evidence(Map.of(
                        "gross_margin_current", latest.getGrossMargin(),
                        "op_margin_current", latest.getOpMargin(),
                        "max_gross_op_divergence", maxDivergence,
                        "consecutive_quarters", consecutiveCount
                ))
                .fiscalYear(latest.getFiscalYear())
                .fiscalQuarter(latest.getFiscalQuarter())
                .severityMetric("op_margin")
                .rawMetricValue(latest.getOpMargin())
                .build());
    }

    /** gross margin 개선 + op margin 악화 동시 발생 시 divergence 크기 반환 */
    private BigDecimal computeDivergence(FinancialDerivedMetrics prev, FinancialDerivedMetrics curr) {
        if (curr.getGrossMargin() == null || prev.getGrossMargin() == null
                || curr.getOpMargin() == null || prev.getOpMargin() == null) return null;
        BigDecimal grossDelta = curr.getGrossMargin().subtract(prev.getGrossMargin());
        BigDecimal opDelta = curr.getOpMargin().subtract(prev.getOpMargin());
        if (grossDelta.compareTo(BigDecimal.ZERO) <= 0 || opDelta.compareTo(BigDecimal.ZERO) >= 0) return null;
        return grossDelta.subtract(opDelta);
    }
}

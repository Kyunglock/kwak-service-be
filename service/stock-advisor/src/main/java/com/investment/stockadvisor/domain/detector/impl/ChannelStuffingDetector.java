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
public class ChannelStuffingDetector implements DivergenceDetector {

    private static final String TYPE = "CHANNEL_STUFFING";
    private static final BigDecimal REVENUE_QOQ_THRESHOLD = new BigDecimal("0.15");
    private static final BigDecimal DSO_INCREASE_THRESHOLD = new BigDecimal("0.15");
    private static final BigDecimal SEVERITY_RANGE = new BigDecimal("0.35");

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
        BigDecimal maxDsoIncrease = BigDecimal.ZERO;

        for (int i = 1; i < sorted.size(); i++) {
            FinancialDerivedMetrics prev = sorted.get(i - 1);
            FinancialDerivedMetrics curr = sorted.get(i);
            BigDecimal dsoIncrease = computeDsoIncrease(prev, curr);

            if (dsoIncrease != null
                    && curr.getRevenueQoq() != null
                    && curr.getRevenueQoq().compareTo(REVENUE_QOQ_THRESHOLD) > 0
                    && dsoIncrease.compareTo(DSO_INCREASE_THRESHOLD) > 0) {
                consecutiveCount++;
                latest = curr;
                if (dsoIncrease.compareTo(maxDsoIncrease) > 0) maxDsoIncrease = dsoIncrease;
            } else {
                consecutiveCount = 0;
            }

            if (consecutiveCount >= 2) break;
        }

        if (consecutiveCount < 2 || latest == null) return Optional.empty();

        BigDecimal severity = maxDsoIncrease.subtract(DSO_INCREASE_THRESHOLD)
                .divide(SEVERITY_RANGE, 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);

        return Optional.of(DetectionResult.builder()
                .type(TYPE)
                .severity(severity)
                .evidence(Map.of(
                        "revenue_qoq", latest.getRevenueQoq(),
                        "dso_current", latest.getDso(),
                        "dso_increase_pct", maxDsoIncrease,
                        "consecutive_quarters", consecutiveCount
                ))
                .fiscalYear(latest.getFiscalYear())
                .fiscalQuarter(latest.getFiscalQuarter())
                .severityMetric("dso")
                .rawMetricValue(latest.getDso())
                .build());
    }

    private BigDecimal computeDsoIncrease(FinancialDerivedMetrics prev, FinancialDerivedMetrics curr) {
        if (prev.getDso() == null || curr.getDso() == null) return null;
        if (prev.getDso().compareTo(BigDecimal.ZERO) == 0) return null;
        return curr.getDso().subtract(prev.getDso())
                .divide(prev.getDso().abs(), 4, RoundingMode.HALF_UP);
    }
}

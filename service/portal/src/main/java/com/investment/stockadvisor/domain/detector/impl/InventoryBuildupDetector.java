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
public class InventoryBuildupDetector implements DivergenceDetector {

    private static final String TYPE = "INVENTORY_BUILDUP";
    private static final int CONSECUTIVE_QUARTERS = 3;
    private static final BigDecimal REVENUE_YOY_FLAT_THRESHOLD = new BigDecimal("0.05");
    private static final BigDecimal SEVERITY_BASE = new BigDecimal("0.20");
    private static final BigDecimal SEVERITY_RANGE = new BigDecimal("0.40");

    @Override
    public String getType() { return TYPE; }

    @Override
    public Optional<DetectionResult> detect(List<FinancialDerivedMetrics> timeSeries) {
        if (timeSeries.size() < CONSECUTIVE_QUARTERS + 1) return Optional.empty();

        List<FinancialDerivedMetrics> sorted = timeSeries.stream()
                .sorted(Comparator.comparing(FinancialDerivedMetrics::getFiscalYear)
                        .thenComparing(FinancialDerivedMetrics::getFiscalQuarter))
                .toList();

        int n = sorted.size();
        for (int i = n - CONSECUTIVE_QUARTERS; i < n; i++) {
            FinancialDerivedMetrics prev = sorted.get(i - 1);
            FinancialDerivedMetrics curr = sorted.get(i);
            if (curr.getDio() == null || prev.getDio() == null) return Optional.empty();
            if (curr.getDio().compareTo(prev.getDio()) <= 0) return Optional.empty();
        }

        FinancialDerivedMetrics latest = sorted.get(n - 1);
        FinancialDerivedMetrics baseline = sorted.get(n - CONSECUTIVE_QUARTERS - 1);

        if (latest.getRevenueYoy() == null) return Optional.empty();
        if (latest.getRevenueYoy().abs().compareTo(REVENUE_YOY_FLAT_THRESHOLD) > 0) return Optional.empty();

        BigDecimal dioGrowth = BigDecimal.ZERO;
        if (baseline.getDio() != null && baseline.getDio().compareTo(BigDecimal.ZERO) != 0) {
            dioGrowth = latest.getDio().subtract(baseline.getDio())
                    .divide(baseline.getDio().abs(), 4, RoundingMode.HALF_UP);
        }

        BigDecimal severity = dioGrowth.subtract(SEVERITY_BASE)
                .divide(SEVERITY_RANGE, 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);

        return Optional.of(DetectionResult.builder()
                .type(TYPE)
                .severity(severity)
                .evidence(Map.of(
                        "dio_current", latest.getDio(),
                        "dio_baseline_3q", baseline.getDio() != null ? baseline.getDio() : BigDecimal.ZERO,
                        "dio_growth_3q", dioGrowth,
                        "revenue_yoy", latest.getRevenueYoy()
                ))
                .fiscalYear(latest.getFiscalYear())
                .fiscalQuarter(latest.getFiscalQuarter())
                .severityMetric("dio")
                .rawMetricValue(latest.getDio())
                .build());
    }
}

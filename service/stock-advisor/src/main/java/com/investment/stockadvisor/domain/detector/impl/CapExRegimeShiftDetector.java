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
public class CapExRegimeShiftDetector implements DivergenceDetector {

    private static final String TYPE = "CAPEX_REGIME_SHIFT";
    private static final BigDecimal SINGLE_QUARTER_SHIFT_THRESHOLD = new BigDecimal("0.50");
    private static final BigDecimal SUSTAINED_HIGH_THRESHOLD = new BigDecimal("3.0");
    private static final BigDecimal SUSTAINED_SEVERITY_RANGE = new BigDecimal("2.0");

    @Override
    public String getType() { return TYPE; }

    @Override
    public Optional<DetectionResult> detect(List<FinancialDerivedMetrics> timeSeries) {
        if (timeSeries.size() < 3) return Optional.empty();

        List<FinancialDerivedMetrics> sorted = timeSeries.stream()
                .sorted(Comparator.comparing(FinancialDerivedMetrics::getFiscalYear)
                        .thenComparing(FinancialDerivedMetrics::getFiscalQuarter))
                .toList();

        // Signal 1: sustained high CapEx ratio (2+ consecutive quarters)
        int sustainedCount = 0;
        FinancialDerivedMetrics latestSustained = null;
        for (FinancialDerivedMetrics m : sorted) {
            if (m.getCapexToDepreciation() != null
                    && m.getCapexToDepreciation().compareTo(SUSTAINED_HIGH_THRESHOLD) > 0) {
                sustainedCount++;
                latestSustained = m;
            } else {
                sustainedCount = 0;
                latestSustained = null;
            }
        }

        if (sustainedCount >= 2 && latestSustained != null) {
            BigDecimal ratio = latestSustained.getCapexToDepreciation();
            BigDecimal severity = ratio.subtract(SUSTAINED_HIGH_THRESHOLD)
                    .divide(SUSTAINED_SEVERITY_RANGE, 4, RoundingMode.HALF_UP)
                    .min(BigDecimal.ONE).max(BigDecimal.ZERO);

            return Optional.of(DetectionResult.builder()
                    .type(TYPE)
                    .severity(severity)
                    .evidence(Map.of(
                            "capex_to_depreciation", ratio,
                            "signal", "SUSTAINED_HIGH",
                            "sustained_quarters", sustainedCount
                    ))
                    .fiscalYear(latestSustained.getFiscalYear())
                    .fiscalQuarter(latestSustained.getFiscalQuarter())
                    .severityMetric("capex_to_depreciation")
                    .rawMetricValue(ratio)
                    .build());
        }

        // Signal 2: single-quarter spike (ratio changes by > 50%)
        for (int i = 1; i < sorted.size(); i++) {
            FinancialDerivedMetrics prev = sorted.get(i - 1);
            FinancialDerivedMetrics curr = sorted.get(i);
            if (prev.getCapexToDepreciation() == null || curr.getCapexToDepreciation() == null) continue;
            if (prev.getCapexToDepreciation().compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal changePct = curr.getCapexToDepreciation().subtract(prev.getCapexToDepreciation())
                    .divide(prev.getCapexToDepreciation().abs(), 4, RoundingMode.HALF_UP);

            if (changePct.abs().compareTo(SINGLE_QUARTER_SHIFT_THRESHOLD) > 0) {
                BigDecimal severity = changePct.abs().subtract(SINGLE_QUARTER_SHIFT_THRESHOLD)
                        .divide(BigDecimal.ONE, 4, RoundingMode.HALF_UP)
                        .min(BigDecimal.ONE).max(BigDecimal.ZERO);

                return Optional.of(DetectionResult.builder()
                        .type(TYPE)
                        .severity(severity)
                        .evidence(Map.of(
                                "prior_ratio", prev.getCapexToDepreciation(),
                                "current_ratio", curr.getCapexToDepreciation(),
                                "change_pct", changePct,
                                "signal", "SINGLE_QUARTER_SPIKE"
                        ))
                        .fiscalYear(curr.getFiscalYear())
                        .fiscalQuarter(curr.getFiscalQuarter())
                        .severityMetric("capex_to_depreciation")
                        .rawMetricValue(curr.getCapexToDepreciation())
                        .build());
            }
        }

        return Optional.empty();
    }
}

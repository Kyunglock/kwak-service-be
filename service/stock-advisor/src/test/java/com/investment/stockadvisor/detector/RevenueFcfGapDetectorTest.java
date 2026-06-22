package com.investment.stockadvisor.detector;

import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.impl.RevenueFcfGapDetector;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RevenueFcfGapDetectorTest {

    private RevenueFcfGapDetector detector;

    @BeforeEach
    void setUp() {
        detector = new RevenueFcfGapDetector();
    }

    @Test
    void detect_returns_empty_when_gap_below_threshold() {
        assertThat(detector.detect(List.of(
            metrics(2024, 1, "0.10", "0.05"),
            metrics(2024, 2, "0.12", "0.07")
        ))).isEmpty();
    }

    @Test
    void detect_returns_result_when_two_consecutive_exceed() {
        Optional<DetectionResult> result = detector.detect(List.of(
            metrics(2024, 1, "0.30", "0.05"),
            metrics(2024, 2, "0.35", "0.08")
        ));

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo("REVENUE_FCF_GAP");
        assertThat(result.get().getFiscalQuarter()).isEqualTo(2);
    }

    @Test
    void detect_returns_empty_when_only_latest_exceeds() {
        assertThat(detector.detect(List.of(
            metrics(2024, 1, "0.15", "0.10"),
            metrics(2024, 2, "0.35", "0.05")
        ))).isEmpty();
    }

    @Test
    void detect_returns_empty_when_null_growth_values() {
        assertThat(detector.detect(List.of(
            FinancialDerivedMetrics.builder().stockCd("MSFT").fiscalYear(2024).fiscalQuarter(1).build(),
            metrics(2024, 2, "0.35", "0.05")
        ))).isEmpty();
    }

    @Test
    void severity_is_clamped_to_one() {
        BigDecimal severity = detector.detect(List.of(
            metrics(2024, 1, "0.80", "-0.10"),
            metrics(2024, 2, "0.90", "-0.20")
        )).orElseThrow().getSeverity();

        assertThat(severity).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void evidence_contains_gap_values() {
        DetectionResult result = detector.detect(List.of(
            metrics(2024, 1, "0.30", "0.05"),
            metrics(2024, 2, "0.35", "0.08")
        )).orElseThrow();

        assertThat(result.getEvidence())
            .containsKeys("gap_current", "gap_prev", "revenueYoy_current", "fcfYoy_current");
    }

    private FinancialDerivedMetrics metrics(int year, int quarter, String revYoy, String fcfYoy) {
        return FinancialDerivedMetrics.builder()
            .stockCd("MSFT").fiscalYear(year).fiscalQuarter(quarter)
            .revenueYoy(new BigDecimal(revYoy))
            .fcfYoy(new BigDecimal(fcfYoy))
            .build();
    }
}

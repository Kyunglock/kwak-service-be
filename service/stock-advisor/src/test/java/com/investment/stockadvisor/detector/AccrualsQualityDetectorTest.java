package com.investment.stockadvisor.detector;

import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.impl.AccrualsQualityDetector;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccrualsQualityDetectorTest {

    private AccrualsQualityDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AccrualsQualityDetector();
    }

    @Test
    void detect_returns_empty_when_only_one_quarter() {
        assertThat(detector.detect(List.of(gap(2024, 1, "0.15")))).isEmpty();
    }

    @Test
    void detect_returns_empty_when_only_latest_exceeds_threshold() {
        assertThat(detector.detect(List.of(
            gap(2024, 1, "0.05"),
            gap(2024, 2, "0.15")
        ))).isEmpty();
    }

    @Test
    void detect_returns_empty_when_only_prev_exceeds_threshold() {
        assertThat(detector.detect(List.of(
            gap(2024, 1, "0.15"),
            gap(2024, 2, "0.05")
        ))).isEmpty();
    }

    @Test
    void detect_returns_result_when_two_consecutive_quarters_exceed() {
        Optional<DetectionResult> result = detector.detect(List.of(
            gap(2024, 1, "0.12"),
            gap(2024, 2, "0.18")
        ));

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo("ACCRUALS_QUALITY");
        assertThat(result.get().getFiscalYear()).isEqualTo(2024);
        assertThat(result.get().getFiscalQuarter()).isEqualTo(2);
    }

    @Test
    void detect_uses_two_most_recent_quarters() {
        // Q1 below, Q2~Q3 above: should detect
        assertThat(detector.detect(List.of(
            gap(2024, 1, "0.05"),
            gap(2024, 2, "0.12"),
            gap(2024, 3, "0.18")
        ))).isPresent();
    }

    @Test
    void severity_is_clamped_to_one() {
        BigDecimal severity = detector.detect(List.of(
            gap(2024, 1, "0.50"),
            gap(2024, 2, "0.60")
        )).orElseThrow().getSeverity();

        assertThat(severity).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void evidence_contains_required_keys() {
        DetectionResult result = detector.detect(List.of(
            gap(2024, 1, "0.12"),
            gap(2024, 2, "0.18")
        )).orElseThrow();

        assertThat(result.getEvidence())
            .containsKeys("niOcfGap_current", "niOcfGap_prev", "threshold", "consecutive_quarters");
    }

    private FinancialDerivedMetrics gap(int year, int quarter, String value) {
        return FinancialDerivedMetrics.builder()
            .stockCd("AAPL").fiscalYear(year).fiscalQuarter(quarter)
            .niOcfGap(new BigDecimal(value)).build();
    }
}

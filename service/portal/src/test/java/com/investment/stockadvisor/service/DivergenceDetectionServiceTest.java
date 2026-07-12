package com.investment.stockadvisor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.stockadvisor.application.service.divergence.DivergenceDetectionConverter;
import com.investment.stockadvisor.application.service.divergence.DivergenceDetectionServiceImpl;
import com.investment.stockadvisor.application.service.divergence.SectorPercentileSeverityCalculator;
import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.DivergenceDetector;
import com.investment.stockadvisor.domain.entity.divergence.DivergenceDetectionResult;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import com.investment.stockadvisor.domain.repository.divergence.DivergenceDetectionResultMapper;
import com.investment.stockadvisor.domain.repository.divergence.FinancialDerivedMetricsMapper;
import com.investment.stockadvisor.domain.repository.divergence.FinancialStatementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DivergenceDetectionServiceTest {

    @Mock FinancialStatementMapper statementMapper;
    @Mock FinancialDerivedMetricsMapper metricsMapper;
    @Mock DivergenceDetectionResultMapper resultMapper;
    @Mock DivergenceDetector detector;
    @Mock DivergenceDetectionConverter converter;
    @Mock SectorPercentileSeverityCalculator sectorPercentileCalculator;

    DivergenceDetectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DivergenceDetectionServiceImpl(
            statementMapper, metricsMapper, resultMapper,
            List.of(detector), converter, new ObjectMapper(),
            sectorPercentileCalculator
        );
    }

    @Test
    void detectAndSave_saves_result_when_detector_finds_anomaly() {
        List<FinancialDerivedMetrics> ts = List.of(metrics(2024, 1), metrics(2024, 2));
        when(metricsMapper.findRecentByStockCd("AAPL", 8)).thenReturn(ts);
        when(detector.detect(ts)).thenReturn(Optional.of(detectionResult()));

        service.detectAndSave("AAPL");

        ArgumentCaptor<DivergenceDetectionResult> captor =
            ArgumentCaptor.forClass(DivergenceDetectionResult.class);
        verify(resultMapper).insert(captor.capture());
        assertThat(captor.getValue().getStockCd()).isEqualTo("AAPL");
        assertThat(captor.getValue().getDivergenceType()).isEqualTo("TEST_TYPE");
    }

    @Test
    void detectAndSave_does_not_save_when_detector_finds_nothing() {
        List<FinancialDerivedMetrics> ts = List.of(metrics(2024, 1), metrics(2024, 2));
        when(metricsMapper.findRecentByStockCd("AAPL", 8)).thenReturn(ts);
        when(detector.detect(ts)).thenReturn(Optional.empty());

        service.detectAndSave("AAPL");

        verifyNoInteractions(resultMapper);
    }

    @Test
    void detectAndSave_skips_when_insufficient_time_series() {
        when(metricsMapper.findRecentByStockCd("AAPL", 8)).thenReturn(List.of(metrics(2024, 1)));

        service.detectAndSave("AAPL");

        verifyNoInteractions(detector);
    }

    @Test
    void detectAndSaveAll_iterates_all_stock_codes() {
        when(statementMapper.findAllStockCds()).thenReturn(List.of("AAPL", "MSFT"));
        when(metricsMapper.findRecentByStockCd(any(), eq(8))).thenReturn(List.of());

        service.detectAndSaveAll();

        verify(metricsMapper).findRecentByStockCd("AAPL", 8);
        verify(metricsMapper).findRecentByStockCd("MSFT", 8);
    }

    private FinancialDerivedMetrics metrics(int year, int quarter) {
        return FinancialDerivedMetrics.builder()
            .stockCd("AAPL").fiscalYear(year).fiscalQuarter(quarter).build();
    }

    private DetectionResult detectionResult() {
        return DetectionResult.builder()
            .type("TEST_TYPE").severity(new BigDecimal("0.5000"))
            .evidence(Map.of("key", "value"))
            .fiscalYear(2024).fiscalQuarter(2).build();
    }
}

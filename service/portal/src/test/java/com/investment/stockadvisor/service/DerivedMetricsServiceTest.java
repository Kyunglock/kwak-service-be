package com.investment.stockadvisor.service;

import com.investment.stockadvisor.application.service.divergence.DerivedMetricsServiceImpl;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import com.investment.stockadvisor.domain.entity.divergence.FinancialStatement;
import com.investment.stockadvisor.domain.repository.divergence.FinancialDerivedMetricsMapper;
import com.investment.stockadvisor.domain.repository.divergence.FinancialStatementMapper;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DerivedMetricsServiceTest {

    @Mock FinancialStatementMapper statementMapper;
    @Mock FinancialDerivedMetricsMapper metricsMapper;

    DerivedMetricsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DerivedMetricsServiceImpl(statementMapper, metricsMapper);
    }

    @Test
    void computeAndSave_skips_when_less_than_two_statements() {
        when(statementMapper.findRecentByStockCd("AAPL", 10))
            .thenReturn(List.of(stmt(2024, 1).build()));

        service.computeAndSave("AAPL");

        verifyNoInteractions(metricsMapper);
    }

    @Test
    void computeAndSave_computes_ni_ocf_gap() {
        when(statementMapper.findRecentByStockCd("AAPL", 10)).thenReturn(List.of(
            stmt(2023, 4).netIncome(bd("100")).operatingCashFlow(bd("80")).totalAssets(bd("1000")).revenue(bd("500")).build(),
            stmt(2024, 1).netIncome(bd("120")).operatingCashFlow(bd("90")).totalAssets(bd("1100")).revenue(bd("550")).build()
        ));

        service.computeAndSave("AAPL");

        ArgumentCaptor<FinancialDerivedMetrics> captor = ArgumentCaptor.forClass(FinancialDerivedMetrics.class);
        verify(metricsMapper).upsert(captor.capture());
        // niOcfGap = (120 - 90) / 1100 = 0.027272...
        assertThat(captor.getValue().getNiOcfGap().doubleValue())
            .isCloseTo(0.0273, Offset.offset(0.0001));
    }

    @Test
    void computeAndSave_computes_qoq_growth() {
        when(statementMapper.findRecentByStockCd("AAPL", 10)).thenReturn(List.of(
            stmt(2023, 4).revenue(bd("500")).build(),
            stmt(2024, 1).revenue(bd("550")).build()
        ));

        service.computeAndSave("AAPL");

        ArgumentCaptor<FinancialDerivedMetrics> captor = ArgumentCaptor.forClass(FinancialDerivedMetrics.class);
        verify(metricsMapper).upsert(captor.capture());
        // QoQ = (550 - 500) / 500 = 0.10
        assertThat(captor.getValue().getRevenueQoq().doubleValue())
            .isCloseTo(0.10, Offset.offset(0.0001));
    }

    @Test
    void computeAndSave_computes_yoy_when_prior_year_same_quarter_exists() {
        when(statementMapper.findRecentByStockCd("AAPL", 10)).thenReturn(List.of(
            stmt(2023, 1).revenue(bd("400")).build(),
            stmt(2023, 4).revenue(bd("500")).build(),
            stmt(2024, 1).revenue(bd("480")).build()
        ));

        service.computeAndSave("AAPL");

        ArgumentCaptor<FinancialDerivedMetrics> captor = ArgumentCaptor.forClass(FinancialDerivedMetrics.class);
        verify(metricsMapper, times(2)).upsert(captor.capture());
        // YoY for 2024 Q1 = (480 - 400) / 400 = 0.20
        assertThat(captor.getAllValues().get(1).getRevenueYoy().doubleValue())
            .isCloseTo(0.20, Offset.offset(0.0001));
    }

    @Test
    void computeAndSave_returns_null_for_metrics_when_required_fields_absent() {
        when(statementMapper.findRecentByStockCd("AAPL", 10)).thenReturn(List.of(
            stmt(2023, 4).revenue(bd("500")).build(),
            stmt(2024, 1).revenue(bd("550")).build()
        ));

        service.computeAndSave("AAPL");

        ArgumentCaptor<FinancialDerivedMetrics> captor = ArgumentCaptor.forClass(FinancialDerivedMetrics.class);
        verify(metricsMapper).upsert(captor.capture());
        assertThat(captor.getValue().getNiOcfGap()).isNull();
        assertThat(captor.getValue().getDso()).isNull();
    }

    private FinancialStatement.FinancialStatementBuilder stmt(int year, int quarter) {
        return FinancialStatement.builder()
            .stockCd("AAPL").fiscalYear(year).fiscalQuarter(quarter)
            .periodEndDt(LocalDate.of(year, quarter * 3, 1));
    }

    private BigDecimal bd(String val) { return new BigDecimal(val); }
}

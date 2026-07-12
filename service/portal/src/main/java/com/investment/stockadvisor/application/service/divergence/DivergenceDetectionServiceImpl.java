package com.investment.stockadvisor.application.service.divergence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.stockadvisor.application.dto.divergence.DivergenceResultResponse;
import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.DivergenceDetector;
import com.investment.stockadvisor.domain.entity.divergence.DivergenceDetectionResult;
import com.investment.stockadvisor.domain.repository.divergence.DivergenceDetectionResultMapper;
import com.investment.stockadvisor.domain.repository.divergence.FinancialDerivedMetricsMapper;
import com.investment.stockadvisor.domain.repository.divergence.FinancialStatementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DivergenceDetectionServiceImpl implements DivergenceDetectionService {

    private final FinancialStatementMapper financialStatementMapper;
    private final FinancialDerivedMetricsMapper derivedMetricsMapper;
    private final DivergenceDetectionResultMapper resultMapper;
    private final List<DivergenceDetector> detectors;
    private final DivergenceDetectionConverter converter;
    private final ObjectMapper objectMapper;
    private final SectorPercentileSeverityCalculator sectorPercentileCalculator;

    private static final int TIME_SERIES_LIMIT = 8;

    @Override
    public void detectAndSaveAll() {
        financialStatementMapper.findAllStockCds()
                .forEach(this::detectAndSave);
    }

    @Override
    public void detectAndSave(String stockCd) {
        var timeSeries = derivedMetricsMapper.findRecentByStockCd(stockCd, TIME_SERIES_LIMIT);
        if (timeSeries.size() < 2) return;

        LocalDate today = LocalDate.now();
        for (DivergenceDetector detector : detectors) {
            detector.detect(timeSeries).ifPresent(result ->
                    resultMapper.insert(toEntity(stockCd, result, today))
            );
        }
    }

    @Override
    public List<DivergenceResultResponse> findByStockCd(String stockCd) {
        return converter.toResponseList(resultMapper.findByStockCd(stockCd));
    }

    @Override
    public List<DivergenceResultResponse> findByBatchRunDt(LocalDate batchRunDt) {
        return converter.toResponseList(resultMapper.findByBatchRunDt(batchRunDt));
    }

    private DivergenceDetectionResult toEntity(String stockCd, DetectionResult result, LocalDate batchRunDt) {
        BigDecimal severity = resolveSeverity(stockCd, result);

        String evidenceJson;
        try {
            evidenceJson = objectMapper.writeValueAsString(result.getEvidence());
        } catch (JsonProcessingException e) {
            evidenceJson = "{}";
        }

        return DivergenceDetectionResult.builder()
                .stockCd(stockCd)
                .fiscalYear(result.getFiscalYear())
                .fiscalQuarter(result.getFiscalQuarter())
                .divergenceType(result.getType())
                .severity(severity)
                .evidence(evidenceJson)
                .detectedAt(LocalDateTime.now())
                .batchRunDt(batchRunDt)
                .build();
    }

    /**
     * 섹터 피어 데이터가 충분하면 퍼센타일 severity 사용, 그렇지 않으면 detector 의 linear clamp 값으로 fallback.
     */
    private BigDecimal resolveSeverity(String stockCd, DetectionResult result) {
        if (result.getSeverityMetric() != null && result.getRawMetricValue() != null) {
            BigDecimal percentileSeverity = sectorPercentileCalculator.computeSeverity(
                    stockCd, result.getSeverityMetric(), result.getRawMetricValue());
            if (percentileSeverity != null) return percentileSeverity;
        }
        return result.getSeverity();
    }
}

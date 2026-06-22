package com.investment.stockadvisor.domain.detector.impl;

import com.investment.stockadvisor.domain.detector.DetectionResult;
import com.investment.stockadvisor.domain.detector.DivergenceDetector;
import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import com.investment.stockadvisor.domain.repository.divergence.FinancialDerivedMetricsMapper;
import com.investment.stockadvisor.domain.repository.divergence.StockSectorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PeerOutlierDetector implements DivergenceDetector {

    private static final String TYPE = "PEER_OUTLIER";
    private static final BigDecimal Z_SCORE_THRESHOLD = new BigDecimal("2.0");
    private static final int MIN_PEER_COUNT = 5;

    private final StockSectorMapper stockSectorMapper;
    private final FinancialDerivedMetricsMapper metricsMapper;

    @Override
    public String getType() { return TYPE; }

    @Override
    public Optional<DetectionResult> detect(List<FinancialDerivedMetrics> timeSeries) {
        if (timeSeries.isEmpty()) return Optional.empty();

        List<FinancialDerivedMetrics> sorted = timeSeries.stream()
                .sorted(Comparator.comparing(FinancialDerivedMetrics::getFiscalYear)
                        .thenComparing(FinancialDerivedMetrics::getFiscalQuarter))
                .toList();

        FinancialDerivedMetrics latest = sorted.get(sorted.size() - 1);
        if (latest.getOpMargin() == null) return Optional.empty();

        String stockCd = latest.getStockCd();
        String sectorCode = stockSectorMapper.findSectorCodeByStockCd(stockCd);
        if (sectorCode == null) return Optional.empty();

        List<String> peers = stockSectorMapper.findStockCdsBySectorCode(sectorCode);
        if (peers.size() < MIN_PEER_COUNT) return Optional.empty();

        List<BigDecimal> sectorOpMargins = peers.stream()
                .filter(p -> !p.equals(stockCd))
                .map(p -> {
                    List<FinancialDerivedMetrics> s = metricsMapper.findRecentByStockCd(p, 1);
                    return s.isEmpty() ? null : s.get(0).getOpMargin();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (sectorOpMargins.size() < MIN_PEER_COUNT) return Optional.empty();

        BigDecimal zScore = computeZScore(latest.getOpMargin(), sectorOpMargins);
        if (zScore == null || zScore.abs().compareTo(Z_SCORE_THRESHOLD) <= 0) return Optional.empty();

        BigDecimal severity = zScore.abs().subtract(Z_SCORE_THRESHOLD)
                .divide(new BigDecimal("2.0"), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);

        DoubleSummaryStatistics stats = sectorOpMargins.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .summaryStatistics();

        return Optional.of(DetectionResult.builder()
                .type(TYPE)
                .severity(severity)
                .evidence(Map.of(
                        "op_margin", latest.getOpMargin(),
                        "sector_mean_op_margin", BigDecimal.valueOf(stats.getAverage()).setScale(4, RoundingMode.HALF_UP),
                        "z_score", zScore,
                        "peer_count", sectorOpMargins.size(),
                        "sector_code", sectorCode
                ))
                .fiscalYear(latest.getFiscalYear())
                .fiscalQuarter(latest.getFiscalQuarter())
                .severityMetric("op_margin")
                .rawMetricValue(latest.getOpMargin())
                .build());
    }

    private BigDecimal computeZScore(BigDecimal value, List<BigDecimal> population) {
        double mean = population.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = population.stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return null;
        double zScore = (value.doubleValue() - mean) / stdDev;
        return BigDecimal.valueOf(zScore).setScale(4, RoundingMode.HALF_UP);
    }
}

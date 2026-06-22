package com.investment.stockadvisor.application.service.divergence;

import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import com.investment.stockadvisor.domain.repository.divergence.FinancialDerivedMetricsMapper;
import com.investment.stockadvisor.domain.repository.divergence.StockSectorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SectorPercentileSeverityCalculator {

    private static final int MIN_PEER_COUNT = 10;

    private final StockSectorMapper stockSectorMapper;
    private final FinancialDerivedMetricsMapper metricsMapper;

    /**
     * severity = |percentile(value, sector_distribution) - 0.5| × 2
     * 섹터 정보 없거나 피어 수 부족 시 null 반환 → 호출자가 linear clamp 로 fallback.
     */
    public BigDecimal computeSeverity(String stockCd, String metricName, BigDecimal rawValue) {
        String sectorCode = stockSectorMapper.findSectorCodeByStockCd(stockCd);
        if (sectorCode == null) return null;

        List<String> peers = stockSectorMapper.findStockCdsBySectorCode(sectorCode);
        if (peers.size() < MIN_PEER_COUNT) return null;

        List<BigDecimal> sectorValues = peers.stream()
                .filter(p -> !p.equals(stockCd))
                .map(p -> {
                    List<FinancialDerivedMetrics> series = metricsMapper.findRecentByStockCd(p, 1);
                    return series.isEmpty() ? null : extractMetric(series.get(0), metricName);
                })
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (sectorValues.size() < MIN_PEER_COUNT) return null;

        long rank = sectorValues.stream().filter(v -> v.compareTo(rawValue) < 0).count();
        double percentile = (double) rank / sectorValues.size();
        double severity = Math.abs(percentile - 0.5) * 2.0;

        return BigDecimal.valueOf(severity).setScale(4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);
    }

    private BigDecimal extractMetric(FinancialDerivedMetrics m, String metricName) {
        return switch (metricName) {
            case "op_margin"             -> m.getOpMargin();
            case "gross_margin"          -> m.getGrossMargin();
            case "revenue_yoy"           -> m.getRevenueYoy();
            case "ni_ocf_gap"            -> m.getNiOcfGap();
            case "fcf_margin"            -> m.getFcfMargin();
            case "dso"                   -> m.getDso();
            case "dio"                   -> m.getDio();
            case "capex_to_depreciation" -> m.getCapexToDepreciation();
            case "net_debt_to_ebitda"    -> m.getNetDebtToEbitda();
            default                      -> null;
        };
    }
}

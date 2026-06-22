package com.investment.stockadvisor.application.service.divergence;

import com.investment.stockadvisor.domain.entity.divergence.FinancialDerivedMetrics;
import com.investment.stockadvisor.domain.entity.divergence.FinancialStatement;
import com.investment.stockadvisor.domain.repository.divergence.FinancialDerivedMetricsMapper;
import com.investment.stockadvisor.domain.repository.divergence.FinancialStatementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DerivedMetricsServiceImpl implements DerivedMetricsService {

    private final FinancialStatementMapper financialStatementMapper;
    private final FinancialDerivedMetricsMapper derivedMetricsMapper;

    private static final int FETCH_LIMIT = 10;

    @Override
    public void computeAndSaveAll() {
        financialStatementMapper.findAllStockCds()
            .forEach(this::computeAndSave);
    }

    @Override
    public void computeAndSave(String stockCd) {
        List<FinancialStatement> statements =
            financialStatementMapper.findRecentByStockCd(stockCd, FETCH_LIMIT);
        if (statements.size() < 2) return;

        List<FinancialStatement> sorted = statements.stream()
            .sorted(Comparator.comparing(FinancialStatement::getFiscalYear)
                .thenComparing(FinancialStatement::getFiscalQuarter))
            .toList();

        List<FinancialDerivedMetrics> toSave = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            FinancialStatement cur = sorted.get(i);
            FinancialStatement prev = sorted.get(i - 1);
            FinancialStatement yoyBase = findYoyBase(sorted, cur);
            toSave.add(compute(cur, prev, yoyBase));
        }

        toSave.forEach(derivedMetricsMapper::upsert);
    }

    private FinancialStatement findYoyBase(List<FinancialStatement> sorted, FinancialStatement cur) {
        return sorted.stream()
            .filter(s -> s.getFiscalYear() == cur.getFiscalYear() - 1
                && s.getFiscalQuarter().equals(cur.getFiscalQuarter()))
            .findFirst()
            .orElse(null);
    }

    private FinancialDerivedMetrics compute(FinancialStatement cur,
                                             FinancialStatement prev,
                                             FinancialStatement yoyBase) {
        BigDecimal yoyRevenue = yoyBase != null ? yoyBase.getRevenue() : null;
        BigDecimal yoyFcf     = yoyBase != null ? yoyBase.getFreeCashFlow() : null;
        BigDecimal yoyOcf     = yoyBase != null ? yoyBase.getOperatingCashFlow() : null;

        return FinancialDerivedMetrics.builder()
            .stockCd(cur.getStockCd())
            .fiscalYear(cur.getFiscalYear())
            .fiscalQuarter(cur.getFiscalQuarter())
            .periodEndDt(cur.getPeriodEndDt())
            .revenueYoy(growth(cur.getRevenue(), yoyRevenue))
            .revenueQoq(growth(cur.getRevenue(), prev.getRevenue()))
            .fcfYoy(growth(cur.getFreeCashFlow(), yoyFcf))
            .ocfYoy(growth(cur.getOperatingCashFlow(), yoyOcf))
            .niOcfGap(niOcfGap(cur))
            .dso(dso(cur))
            .dio(dio(cur))
            .grossMargin(margin(cur.getGrossProfit(), cur.getRevenue()))
            .opMargin(margin(cur.getOperatingIncome(), cur.getRevenue()))
            .fcfMargin(margin(cur.getFreeCashFlow(), cur.getRevenue()))
            .capexToDepreciation(ratio(cur.getCapex(), cur.getDepreciation()))
            .rdIntensity(margin(cur.getRdExpense(), cur.getRevenue()))
            .sgaIntensity(margin(cur.getSgaExpense(), cur.getRevenue()))
            .netDebtToEbitda(ratio(cur.getNetDebt(), cur.getEbitda()))
            .build();
    }

    private BigDecimal growth(BigDecimal cur, BigDecimal base) {
        if (cur == null || base == null || base.compareTo(BigDecimal.ZERO) == 0) return null;
        return cur.subtract(base).divide(base.abs(), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal niOcfGap(FinancialStatement s) {
        if (s.getNetIncome() == null || s.getOperatingCashFlow() == null
                || s.getTotalAssets() == null
                || s.getTotalAssets().compareTo(BigDecimal.ZERO) == 0) return null;
        return s.getNetIncome().subtract(s.getOperatingCashFlow())
            .divide(s.getTotalAssets(), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal dso(FinancialStatement s) {
        if (s.getAccountsReceivable() == null || s.getRevenue() == null
                || s.getRevenue().compareTo(BigDecimal.ZERO) == 0) return null;
        return s.getAccountsReceivable()
            .multiply(new BigDecimal("90"))
            .divide(s.getRevenue(), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal dio(FinancialStatement s) {
        if (s.getInventory() == null || s.getCogs() == null
                || s.getCogs().compareTo(BigDecimal.ZERO) == 0) return null;
        return s.getInventory()
            .multiply(new BigDecimal("90"))
            .divide(s.getCogs(), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal margin(BigDecimal numerator, BigDecimal revenue) {
        if (numerator == null || revenue == null
                || revenue.compareTo(BigDecimal.ZERO) == 0) return null;
        return numerator.divide(revenue, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null
                || denominator.compareTo(BigDecimal.ZERO) == 0) return null;
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}

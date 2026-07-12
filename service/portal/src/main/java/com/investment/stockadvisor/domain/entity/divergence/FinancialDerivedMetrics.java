package com.investment.stockadvisor.domain.entity.divergence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialDerivedMetrics {

    private Long id;
    private String stockCd;
    private Integer fiscalYear;
    private Integer fiscalQuarter;
    private LocalDate periodEndDt;

    /** 매출 YoY 성장률 */
    private BigDecimal revenueYoy;
    /** 매출 QoQ 성장률 */
    private BigDecimal revenueQoq;
    /** FCF YoY 성장률 */
    private BigDecimal fcfYoy;
    /** OCF YoY 성장률 */
    private BigDecimal ocfYoy;
    /** (NetIncome - OperatingCashFlow) / TotalAssets */
    private BigDecimal niOcfGap;
    /** AR / Revenue * 90 */
    private BigDecimal dso;
    /** Inventory / COGS * 90 */
    private BigDecimal dio;
    private BigDecimal grossMargin;
    private BigDecimal opMargin;
    private BigDecimal fcfMargin;
    /** CapEx / Depreciation */
    private BigDecimal capexToDepreciation;
    /** R&D / Revenue */
    private BigDecimal rdIntensity;
    /** SG&A / Revenue */
    private BigDecimal sgaIntensity;
    /** NetDebt / EBITDA */
    private BigDecimal netDebtToEbitda;

    private LocalDateTime regDt;
    private LocalDateTime updDt;
}

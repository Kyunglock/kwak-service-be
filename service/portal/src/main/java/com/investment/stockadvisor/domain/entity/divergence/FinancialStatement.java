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
public class FinancialStatement {

    private Long id;
    private String stockCd;
    private Integer fiscalYear;
    private Integer fiscalQuarter;
    private LocalDate periodEndDt;

    private BigDecimal revenue;
    private BigDecimal grossProfit;
    private BigDecimal operatingIncome;
    private BigDecimal netIncome;
    private BigDecimal operatingCashFlow;
    private BigDecimal freeCashFlow;
    private BigDecimal capex;
    private BigDecimal depreciation;
    private BigDecimal totalAssets;
    private BigDecimal accountsReceivable;
    private BigDecimal inventory;
    private BigDecimal cogs;
    private BigDecimal rdExpense;
    private BigDecimal sgaExpense;
    private BigDecimal ebitda;
    private BigDecimal netDebt;

    private LocalDateTime regDt;
    private LocalDateTime updDt;
}

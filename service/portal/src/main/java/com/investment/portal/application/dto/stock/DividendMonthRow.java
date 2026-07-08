package com.investment.portal.application.dto.stock;

import lombok.Getter;
import lombok.Setter;

/** 종목별 배당락월(1~12) 행 — findDividendMonthsByStockCodes 결과. */
@Getter
@Setter
public class DividendMonthRow {
    private String stockCd;
    private Integer divMonth;
}

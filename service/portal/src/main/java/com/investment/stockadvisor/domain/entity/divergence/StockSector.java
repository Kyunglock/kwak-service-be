package com.investment.stockadvisor.domain.entity.divergence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSector {
    private String stockCd;
    private String sectorCode;
    private String sectorName;
}

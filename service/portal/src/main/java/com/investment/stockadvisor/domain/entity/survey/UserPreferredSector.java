package com.investment.stockadvisor.domain.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferredSector {

    private String sector;
    private String sectorKo;
    private BigDecimal sectorPct;
}

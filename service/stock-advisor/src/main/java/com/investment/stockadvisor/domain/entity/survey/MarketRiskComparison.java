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
public class MarketRiskComparison {

    private Integer sortOrder;
    private String questionText;
    private String myOptionText;
    private String majorityOptionText;
    private String matchYn;
    private BigDecimal myChoicePct;
}

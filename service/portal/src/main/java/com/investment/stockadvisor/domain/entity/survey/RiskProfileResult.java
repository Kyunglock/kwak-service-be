package com.investment.stockadvisor.domain.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskProfileResult {

    private String description;
    private Integer score;
}

package com.investment.portal.domain.entity.insight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightResult {
    private Long resultId;
    private String userId;
    private String resultTypeCd;   // KEY_FINDINGS, INVESTMENT_STYLE, RISK_ASSESSMENT, PORTFOLIO_ALIGNMENT, INVESTMENT_RECOMMENDATION
    private String title;
    private String content;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}

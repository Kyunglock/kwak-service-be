package com.investment.survey.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResult {

    private Long resultId;
    private String userId;
    private Long surveyId;
    private Integer riskScore;
    private String riskLevelCode;     // 공통코드: RISK_LEVEL
    private String recommendation;
    private String portfolioSuggestion;
    private LocalDateTime analyzedAt;
    private LocalDateTime validUntil;
    private Boolean useYn;
}

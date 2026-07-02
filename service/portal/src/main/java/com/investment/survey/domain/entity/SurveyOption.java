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
public class SurveyOption {

    private Long optionId;
    private Long questionId;
    private String optionText;
    private String optionValue;
    private Integer sortOrder;
    private Integer score;
    private LocalDateTime regDt;
}

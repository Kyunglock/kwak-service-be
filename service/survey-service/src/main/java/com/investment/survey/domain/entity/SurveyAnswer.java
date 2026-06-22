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
public class SurveyAnswer {

    private Long answerId;
    private Long responseId;
    private Long questionId;
    private Long selectedOptionId;
    private String selectedValue;
    private Integer answerScore;
    private LocalDateTime answeredAt;
}

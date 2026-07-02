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
public class SurveyStats {
    private Long surveyId;
    private Long responseId;
    private String surveyName;
    private LocalDateTime regDt;
    private Integer totalParticipants;
    private String statusCode;
}

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
public class SurveyResponse {
    private Long surveyId;
    private Long responseId;
    private String statusCode;
    private LocalDateTime completedAt;
    private String surveyName;
    private String description;
    private String surveyTypeCode;
    private Integer totalParticipants;
    private LocalDateTime regDt;
}

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
public class UserSurveyResponse {

    private Long responseId;
    private String userId;
    private Long surveyId;
    private LocalDateTime startAt;
    private LocalDateTime completedAt;
    private String statusCode;        // 공통코드: RESPONSE_STATUS
    private Integer totalScore;
    private String riskProfileCode;   // 공통코드: RISK_LEVEL
    private LocalDateTime createdAt;
    
    //
    private Integer totalParticipants; 
}

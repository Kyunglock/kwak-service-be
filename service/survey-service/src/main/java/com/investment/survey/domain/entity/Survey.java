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
public class Survey {

    private Long surveyId;
    private String surveyName;
    private String description;
    private String surveyTypeCode;    // 공통코드: SURVEY_TYPE
    private LocalDateTime regDt;
    private LocalDateTime updDt;
    private Boolean useYn;
}

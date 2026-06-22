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
public class QuestionWithAnswer {

    private Long questionId;
    private Long surveyId;
    private Integer questionNumber;
    private String questionText;
    private String questionTypeCode;  // 공통코드: QUESTION_TYPE
    private String description;
    private Integer sortOrder;
    private LocalDateTime regDt;    
    private Long selectedOptionId;
    private String selectedValue;
}

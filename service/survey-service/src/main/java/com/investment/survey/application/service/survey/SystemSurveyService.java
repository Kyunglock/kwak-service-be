package com.investment.survey.application.service.survey;

import com.investment.survey.application.dto.survey.SurveyDetailResponse;
import com.investment.survey.application.dto.survey.SystemSurveyCreateRequest;

public interface SystemSurveyService {

    /**
     * 설문 + 문항 + 선택지를 단일 트랜잭션으로 생성합니다.
     * 뉴스 크롤러가 AI 분석 결과를 한 번에 저장할 때 사용합니다.
     */
    SurveyDetailResponse createSurveyWithQuestions(SystemSurveyCreateRequest request);
}

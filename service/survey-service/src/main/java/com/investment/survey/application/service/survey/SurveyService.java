package com.investment.survey.application.service.survey;

import com.investment.survey.application.dto.question.QuestionAddRequest;
import com.investment.survey.application.dto.question.QuestionResponse;
import com.investment.survey.application.dto.survey.*;

import java.util.List;

public interface SurveyService {

    // 설문 CRUD
    SurveyResponse getSurvey(Long surveyId);

    SurveyDetailResponse getSurveyDetail(Long surveyId);

    List<SurveyResponse> getSurveys();

    List<SurveyResponse> getSurveysByType(String surveyTypeCode);

    SurveyResponse addSurvey(SurveyAddRequest request);

    SurveyResponse modifySurvey(SurveyModRequest request);

    void removeSurvey(Long surveyId);

    // 문항 + 선택지 관리
    QuestionResponse addQuestion(Long surveyId, QuestionAddRequest request);

    List<QuestionResponse> getQuestions(Long surveyId);

    void removeQuestion(Long questionId);

    SurveyAnswerResponse getSurveyResponseDetail(Long surveyId, Long responseId, String userId);

    SurveyOptionStatResponse getSurveyResponseStatsDetail(Long surveyId, Long responseId, String userId);
}

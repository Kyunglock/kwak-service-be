package com.investment.survey.application.service.survey;

import com.investment.survey.application.dto.response.SurveySubmitRequest;
import com.investment.survey.application.dto.response.SurveySubmitResponse;
import com.investment.survey.application.dto.response.SurveyWithMyResponse;
import com.investment.survey.application.dto.result.SurveyResultResponse;
import kwak.common.application.dto.PageResponse;

import java.util.List;

public interface SurveyResponseService {

    SurveySubmitResponse submitSurvey(String userId, SurveySubmitRequest request);

    List<SurveySubmitResponse> getMyResponses(String userId);

    SurveyResultResponse getMyResult(String userId, Long surveyId);

    List<SurveyResultResponse> getMyResults(String userId);

    List<SurveyWithMyResponse> getSurveyWithMyResponses(String userId);

    PageResponse<SurveyWithMyResponse> getSurveyWithMyResponsesPaged(
            String userId, String keyword, int page, int size, String sort);
}

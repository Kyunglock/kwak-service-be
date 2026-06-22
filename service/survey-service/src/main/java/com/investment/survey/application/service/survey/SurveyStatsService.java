package com.investment.survey.application.service.survey;

import java.util.List;

import com.investment.survey.application.dto.survey.SurveyStatsResponse;

public interface SurveyStatsService {
    List<SurveyStatsResponse> getSurveyStatsResponses(String userId);
}

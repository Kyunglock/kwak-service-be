package com.investment.survey.application.service.survey;

import java.util.List;

import com.investment.survey.application.dto.survey.SurveyStatsResponse;
import kwak.common.application.dto.PageResponse;

public interface SurveyStatsService {
    List<SurveyStatsResponse> getSurveyStatsResponses(String userId);

    PageResponse<SurveyStatsResponse> getSurveyStatsResponsesPaged(
            String userId, String keyword, int page, int size, String sort);
}

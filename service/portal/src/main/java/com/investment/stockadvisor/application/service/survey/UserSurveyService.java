package com.investment.stockadvisor.application.service.survey;

import com.investment.stockadvisor.application.dto.survey.MarketRiskComparisonResponse;
import com.investment.stockadvisor.application.dto.survey.UserPreferredSectorResponse;
import com.investment.stockadvisor.application.dto.survey.UserRiskProfileResponse;

import java.util.List;

public interface UserSurveyService {

    List<UserRiskProfileResponse> findUserRiskProfileResults(String userId);

    List<UserPreferredSectorResponse> findUserPreferredSectors();

    List<MarketRiskComparisonResponse> findMarketRiskComparison(String userId);
}

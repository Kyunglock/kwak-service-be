package com.investment.stockadvisor.application.service.survey;


import com.investment.stockadvisor.application.dto.survey.MarketRiskComparisonResponse;
import com.investment.stockadvisor.application.dto.survey.UserPreferredSectorResponse;
import com.investment.stockadvisor.application.dto.survey.UserRiskProfileResponse;
import com.investment.stockadvisor.domain.entity.survey.MarketRiskComparison;
import com.investment.stockadvisor.domain.entity.survey.RiskProfileResult;
import com.investment.stockadvisor.domain.entity.survey.UserPreferredSector;
import com.investment.stockadvisor.domain.repository.survey.UserSurveyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSurveyServiceImpl implements UserSurveyService {

    private final UserSurveyMapper userSurveyMapper;
    private final SurveyConverter surveyConverter;

    @Override
    public List<UserRiskProfileResponse> findUserRiskProfileResults(String userId) {
        List<RiskProfileResult> results = userSurveyMapper.findUserRiskProfileResults(userId);
        return surveyConverter.toResponseList(results);
    }

    @Override
    public List<UserPreferredSectorResponse> findUserPreferredSectors() {
        List<UserPreferredSector> sectors = userSurveyMapper.findUserPreferredSectors();
        return surveyConverter.toSectorResponseList(sectors);
    }

    @Override
    public List<MarketRiskComparisonResponse> findMarketRiskComparison(String userId) {
        List<MarketRiskComparison> results = userSurveyMapper.findMarketRiskComparison(userId);
        return surveyConverter.toMarketRiskComparisonResponseList(results);
    }
}

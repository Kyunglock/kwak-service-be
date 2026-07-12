package com.investment.stockadvisor.domain.repository.survey;

import com.investment.stockadvisor.domain.entity.survey.MarketRiskComparison;
import com.investment.stockadvisor.domain.entity.survey.RiskProfileResult;
import com.investment.stockadvisor.domain.entity.survey.UserPreferredSector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserSurveyMapper {

    List<RiskProfileResult> findUserRiskProfileResults(@Param("userId") String userId);
    List<UserPreferredSector> findUserPreferredSectors();
    List<MarketRiskComparison> findMarketRiskComparison(@Param("userId") String userId);
}

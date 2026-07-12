package com.investment.stockadvisor.application.service.survey;

import com.investment.stockadvisor.application.dto.survey.MarketRiskComparisonResponse;
import com.investment.stockadvisor.application.dto.survey.UserPreferredSectorResponse;
import com.investment.stockadvisor.application.dto.survey.UserRiskProfileResponse;
import com.investment.stockadvisor.domain.entity.survey.MarketRiskComparison;
import com.investment.stockadvisor.domain.entity.survey.RiskProfileResult;
import com.investment.stockadvisor.domain.entity.survey.UserPreferredSector;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SurveyConverter {

    UserRiskProfileResponse toResponse(RiskProfileResult entity);

    List<UserRiskProfileResponse> toResponseList(List<RiskProfileResult> entity);

    UserPreferredSectorResponse toSectorResponse(UserPreferredSector entity);

    List<UserPreferredSectorResponse> toSectorResponseList(List<UserPreferredSector> entities);

    MarketRiskComparisonResponse toMarketRiskComparisonResponse(MarketRiskComparison entity);

    List<MarketRiskComparisonResponse> toMarketRiskComparisonResponseList(List<MarketRiskComparison> entities);
}

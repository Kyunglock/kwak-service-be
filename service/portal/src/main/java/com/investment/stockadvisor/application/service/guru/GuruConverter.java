package com.investment.stockadvisor.application.service.guru;

import com.investment.stockadvisor.application.dto.guru.GuruPortfolioResponse;
import com.investment.stockadvisor.application.dto.guru.GuruRecentActivityResponse;
import com.investment.stockadvisor.domain.entity.guru.GuruPortfolio;
import com.investment.stockadvisor.domain.entity.guru.GuruRecentActivity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GuruConverter {

    /** GuruPortfolio Entity -> Response DTO 변환 */
    GuruPortfolioResponse toPortfolioResponse(GuruPortfolio entity);

    /** GuruPortfolio Entity List -> Response DTO List 변환 */
    List<GuruPortfolioResponse> toPortfolioResponseList(List<GuruPortfolio> entities);

    /** GuruRecentActivity Entity -> Response DTO 변환 */
    GuruRecentActivityResponse toActivityResponse(GuruRecentActivity entity);

    /** GuruRecentActivity Entity List -> Response DTO List 변환 */
    List<GuruRecentActivityResponse> toActivityResponseList(List<GuruRecentActivity> entities);
}

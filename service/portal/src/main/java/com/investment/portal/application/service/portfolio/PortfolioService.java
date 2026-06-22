package com.investment.portal.application.service.portfolio;

import com.investment.portal.application.dto.portfolio.*;

import java.util.List;

public interface PortfolioService {

    /**
     * 포트폴리오 단건 조회
     */
    PortfolioResponse getPortfolio(Long portfolioId);

    /**
     * 사용자별 포트폴리오 목록 조회
     */
    List<PortfolioResponse> getPortfoliosByUserId(String userId);

    /**
     * 내 포트폴리오 목록 조회
     */
    List<PortfolioResponse> getMyPortfolios(String userId);

    /**
     * 포트폴리오 등록
     */
    PortfolioResponse addPortfolio(String userId, PortfolioAddRequest request);

    /**
     * 포트폴리오 수정
     */
    PortfolioResponse modifyPortfolio(PortfolioModRequest request);

    /**
     * 포트폴리오 삭제 (논리 삭제)
     */
    void removePortfolio(Long portfolioId);
}

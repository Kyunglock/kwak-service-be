package com.investment.portal.application.service.trade;

/** 본인 소유가 아닌 포트폴리오에 접근했을 때. */
public class PortfolioAccessDeniedException extends RuntimeException {
    public PortfolioAccessDeniedException(Long portfolioId) {
        super("해당 포트폴리오에 접근할 수 없습니다: " + portfolioId);
    }
}

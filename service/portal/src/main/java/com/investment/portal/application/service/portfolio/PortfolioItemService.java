package com.investment.portal.application.service.portfolio;

import com.investment.portal.application.dto.portfolio.item.*;

import java.util.List;

public interface PortfolioItemService {

    /**
     * 항목ID로 조회
     */
    PortfolioItemResponse getItem(Long itemId);

    /**
     * 포트폴리오ID로 종목 목록 조회
     */
    List<PortfolioItemResponse> getItemsByPortfolioId(Long portfolioId);

    /**
     * 검색 조건으로 조회
     */
    List<PortfolioItemResponse> searchItems(PortfolioItemSearchRequest request);

    /**
     * 종목 등록
     */
    PortfolioItemResponse addItem(PortfolioItemAddRequest request);

    /**
     * 종목 수정
     */
    PortfolioItemResponse modifyItem(PortfolioItemModRequest request);

    /**
     * 종목 삭제 (논리 삭제)
     */
    void removeItem(Long itemId);
}

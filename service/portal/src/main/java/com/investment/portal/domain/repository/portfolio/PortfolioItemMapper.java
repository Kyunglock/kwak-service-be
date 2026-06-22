package com.investment.portal.domain.repository.portfolio;

import com.investment.portal.application.dto.portfolio.item.PortfolioItemSearchRequest;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PortfolioItemMapper {

    PortfolioItem findByItemId(@Param("itemId") Long itemId);

    List<PortfolioItem> findByPortfolioId(@Param("portfolioId") Long portfolioId);

    PortfolioItem findByPortfolioIdAndStockCd(
            @Param("portfolioId") Long portfolioId,
            @Param("stockCd") String stockCd);

    List<PortfolioItem> search(PortfolioItemSearchRequest request);

    /**
     * 전체 사용자 기준 보유자 수 상위 종목 집계
     * 반환 키: stockCd (String), holderCount (Long)
     */
    List<Map<String, Object>> findTopStocksByHolderCount(@Param("limit") int limit);

    int insert(PortfolioItem portfolioItem);

    int update(PortfolioItem portfolioItem);

    int delete(@Param("itemId") Long itemId);
}

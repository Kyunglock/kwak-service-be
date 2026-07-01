package com.investment.portal.domain.repository.stock;

import com.investment.portal.application.dto.stock.StockContextRow;
import com.investment.portal.application.dto.stock.StockWithLatestPriceResponse;
import com.investment.portal.domain.entity.history.stockPrice.StockPriceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockPriceHistoryMapper {

    /**
     * 특정 종목의 가장 최근 종가 조회
     */
    StockPriceHistory findLatestByStockCd(@Param("stockCd") String stockCd);

    /**
     * 여러 종목의 가장 최근 종가 일괄 조회
     */
    List<StockPriceHistory> findLatestByStockCodes(@Param("stockCodes") List<String> stockCodes);

    /**
     * TBL_COMPANIES와 조인하여 전체 기업의 가장 최근 종가 조회
     */
    List<StockWithLatestPriceResponse> findAllWithLatestPrice();

    /**
     * 인사이트 종목 컨텍스트 조회: 회사 마스터 + 최신 종가 + 최근 1년 고저.
     */
    List<StockContextRow> findStockContextByStockCodes(@Param("stockCodes") List<String> stockCodes);

    /**
     * 주가 이력 등록
     */
    int insert(StockPriceHistory stockPriceHistory);

    /**
     * 주가 이력 일괄 등록
     */
    int batchInsert(@Param("list") List<StockPriceHistory> list);
}

package com.investment.portal.domain.repository.stock;

import com.investment.portal.application.dto.stock.StockContextRow;
import com.investment.portal.application.dto.stock.StockPriceMoveRow;
import com.investment.portal.application.dto.stock.StockWithLatestPriceResponse;
import com.investment.portal.domain.entity.history.stockPrice.StockPriceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * 기간(startDt~endDt) 내 전일 대비 등락률이 가장 큰(direction="GAIN") 또는
     * 가장 작은(direction="DROP") 날. startDt 당일의 등락률도 후보에 넣기 위해
     * LAG 계산 자체는 startDt 이전 데이터까지 함께 본다.
     */
    Optional<StockPriceMoveRow> findMaxChangeDay(
            @Param("stockCd") String stockCd,
            @Param("startDt") LocalDate startDt,
            @Param("endDt") LocalDate endDt,
            @Param("direction") String direction);

    /**
     * dt 이후(포함) 가장 가까운 거래일 종가. 기간 수익률 계산의 시작점.
     */
    Optional<StockPriceHistory> findClosestOnOrAfter(@Param("stockCd") String stockCd, @Param("dt") LocalDate dt);

    /**
     * dt 이전(포함) 가장 가까운 거래일 종가. 기간 수익률 계산의 끝점.
     */
    Optional<StockPriceHistory> findClosestOnOrBefore(@Param("stockCd") String stockCd, @Param("dt") LocalDate dt);

    /**
     * 기간 내 고가가 가장 높았던 날.
     */
    Optional<StockPriceHistory> findPeriodHighDay(
            @Param("stockCd") String stockCd, @Param("startDt") LocalDate startDt, @Param("endDt") LocalDate endDt);

    /**
     * 기간 내 저가가 가장 낮았던 날.
     */
    Optional<StockPriceHistory> findPeriodLowDay(
            @Param("stockCd") String stockCd, @Param("startDt") LocalDate startDt, @Param("endDt") LocalDate endDt);

    /**
     * 주가 이력 등록
     */
    int insert(StockPriceHistory stockPriceHistory);

    /**
     * 주가 이력 일괄 등록
     */
    int batchInsert(@Param("list") List<StockPriceHistory> list);
}

package com.investment.portal.application.service.stock;

import com.investment.portal.application.dto.stock.StockPriceSnapshot;

import java.util.List;

/**
 * 외부 API에서 실시간 주가를 조회하는 서비스 인터페이스
 * 대상: US S&P 500 종목
 */
public interface StockPriceFetchService {

    /**
     * 특정 종목의 실시간 가격 조회
     */
    StockPriceSnapshot fetchPrice(String stockCd);

    /**
     * 여러 종목의 실시간 가격 일괄 조회 (배치)
     */
    List<StockPriceSnapshot> fetchPrices(List<String> stockCodes);
}

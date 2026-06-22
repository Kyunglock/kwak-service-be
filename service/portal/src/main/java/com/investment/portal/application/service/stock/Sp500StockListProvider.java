package com.investment.portal.application.service.stock;

import com.investment.portal.domain.repository.stock.CompaniesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 종목 리스트 제공
 *
 * tbl_companies에 등록된 전체 티커를 동적으로 조회한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Sp500StockListProvider {

    private final CompaniesMapper companiesMapper;

    public List<String> getStockCodes() {
        List<String> tickers = companiesMapper.findAllTickers();
        log.debug("[StockListProvider] DB에서 종목 {}건 조회", tickers.size());
        return tickers;
    }

    public int getStockCount() {
        return companiesMapper.findAllTickers().size();
    }
}

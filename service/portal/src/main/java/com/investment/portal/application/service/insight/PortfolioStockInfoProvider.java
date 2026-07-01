package com.investment.portal.application.service.insight;

import com.investment.portal.application.dto.stock.StockContextRow;
import com.investment.portal.application.dto.stock.StockDividendRow;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import com.investment.portal.domain.repository.dividend.DividendHistoryMapper;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 포트폴리오 보유 종목의 시장 정보를 DB에서 조립한다.
 * 기존 Yahoo Finance 라이브 호출을 대체 — collector가 적재한
 * tbl_companies + tbl_stock_price_history + tbl_dividend_history 사용.
 * PER/시총/industry는 DB에 없어 0/""로 채운다(소비 측에서 "미집계" 처리).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioStockInfoProvider {

    private final StockPriceHistoryMapper stockPriceHistoryMapper;
    private final DividendHistoryMapper   dividendHistoryMapper;

    /** 포트폴리오 아이템 → stockCd별 StockInfo 맵. 가격 데이터 없는 종목은 제외. */
    public Map<String, StockInfo> fetchForItems(List<PortfolioItem> items) {
        List<String> tickers = items.stream()
                .map(PortfolioItem::getStockCd)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tickers.isEmpty()) return Collections.emptyMap();

        Map<String, String> currencyByCode = items.stream()
                .filter(i -> i.getStockCd() != null)
                .collect(Collectors.toMap(
                        PortfolioItem::getStockCd,
                        i -> i.getCurrency() == null ? "" : i.getCurrency(),
                        (a, b) -> a));

        Map<String, BigDecimal> dividendByCode = dividendHistoryMapper
                .findTrailingDividendByStockCodes(tickers).stream()
                .filter(r -> r.getStockCd() != null && r.getTrailingDividend() != null)
                .collect(Collectors.toMap(
                        StockDividendRow::getStockCd,
                        StockDividendRow::getTrailingDividend,
                        (a, b) -> a));

        Map<String, StockInfo> result = new LinkedHashMap<>();
        for (StockContextRow row : stockPriceHistoryMapper.findStockContextByStockCodes(tickers)) {
            if (row.getClosePrice() == null) continue;              // 가격 이력 없는 종목 제외
            double current = row.getClosePrice().doubleValue();
            if (current <= 0) continue;

            double open   = row.getOpenPrice() != null ? row.getOpenPrice().doubleValue() : current;
            double change = open > 0 ? (current - open) / open : 0.0;
            double high   = row.getWeek52High() != null ? row.getWeek52High().doubleValue() : current;
            double low    = row.getWeek52Low()  != null ? row.getWeek52Low().doubleValue()  : current;

            BigDecimal div = dividendByCode.get(row.getStockCd());
            double yield   = div != null ? div.doubleValue() / current : 0.0;

            result.put(row.getStockCd(), new StockInfo(
                    row.getStockCd(),
                    row.getCompanyName() != null ? row.getCompanyName() : row.getStockCd(),
                    row.getSector() != null ? row.getSector() : "",
                    "",                                             // industry (DB 미보유)
                    current,
                    change,
                    0L,                                             // marketCap (DB 미보유)
                    0.0,                                            // peRatio (DB 미보유 → 미집계)
                    yield,
                    low,
                    high,
                    currencyByCode.getOrDefault(row.getStockCd(), "")
            ));
        }

        log.info("[Insight] DB 종목정보 조립 - 요청 {}종목, 확보 {}종목", tickers.size(), result.size());
        return result;
    }
}

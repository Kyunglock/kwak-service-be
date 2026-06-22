package com.investment.portal.application.service.stock;

import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import com.investment.portal.application.dto.stock.StockWithLatestPriceResponse;
import com.investment.portal.domain.entity.history.stockPrice.StockPriceHistory;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import com.investment.portal.infrastructure.cache.StockPriceCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 주가 조회 서비스
 *
 * 조회 전략:
 * 1. 캐시(인메모리) 조회 → 있으면 즉시 반환 (실시간 가격)
 * 2. 캐시 miss → DB에서 최근 종가 조회 (StockPriceHistory)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceQueryService {

    private final StockPriceCacheStore cacheStore;
    private final StockPriceHistoryMapper stockPriceHistoryMapper;
    private final Sp500StockListProvider stockListProvider;

    /**
     * 종목 가격 조회 (캐시 → DB fallback)
     */
    public StockPriceSnapshot getPrice(String stockCd) {
        // 1. 캐시에서 실시간 가격 조회
        StockPriceSnapshot cached = cacheStore.get(stockCd);
        if (cached != null) {
            log.debug("[QueryService] 캐시 hit: {}", stockCd);
            return cached;
        }

        // 2. 캐시 miss → DB에서 최근 종가 조회
        log.debug("[QueryService] 캐시 miss, DB 종가 조회: {}", stockCd);
        StockPriceHistory history = stockPriceHistoryMapper.findLatestByStockCd(stockCd);
        if (history == null) {
            return null;
        }

        return toSnapshotFromHistory(history);
    }

    /**
     * 전체 종목(상위 50개) 가격 조회
     * 캐시에 있는 종목은 캐시에서, 캐시에 빠진 종목은 DB 최신 종가에서 보충
     */
    public Collection<StockPriceSnapshot> getAllPrices() {
        List<String> allStockCodes = stockListProvider.getStockCodes();
        ConcurrentMap<String, StockPriceSnapshot> cacheMap = cacheStore.getAllAsMap();

        // 캐시에 없는 종목 코드 추출
        List<String> missingCodes = allStockCodes.stream()
                .filter(code -> !cacheMap.containsKey(code))
                .collect(Collectors.toList());

        // 캐시 데이터 수집
        Map<String, StockPriceSnapshot> result = new LinkedHashMap<>(cacheMap);

        // 캐시에 빠진 종목은 DB에서 최신 종가 보충
        if (!missingCodes.isEmpty()) {
            log.debug("[QueryService] 캐시 miss {}건, DB 종가 보충: {}", missingCodes.size(), missingCodes);
            List<StockPriceHistory> histories = stockPriceHistoryMapper.findLatestByStockCodes(missingCodes);
            for (StockPriceHistory history : histories) {
                result.put(history.getStockCd(), toSnapshotFromHistory(history));
            }
        }

        log.debug("[QueryService] 전체 조회: 캐시 {}건 + DB 보충 {}건 = 총 {}건",
                cacheMap.size(), result.size() - cacheMap.size(), result.size());

        return result.values();
    }

    /**
     * 지정 종목 코드 목록에 대해 DB 최근 종가를 조회하여 StockPriceSnapshot 목록으로 반환
     * Finnhub 등 외부 스트림에서 누락된 종목을 보충할 때 사용
     */
    public List<StockPriceSnapshot> getPricesFromDb(List<String> stockCodes) {
        if (stockCodes.isEmpty()) {
            return Collections.emptyList();
        }
        log.debug("[QueryService] DB 종가 보충 조회 {}건: {}", stockCodes.size(), stockCodes);
        List<StockPriceHistory> histories = stockPriceHistoryMapper.findLatestByStockCodes(stockCodes);
        return histories.stream()
                .map(this::toSnapshotFromHistory)
                .collect(Collectors.toList());
    }

    /**
     * TBL_COMPANIES + 최근 종가 조인 조회
     */
    public List<StockWithLatestPriceResponse> getAllWithLatestPrice() {
        return stockPriceHistoryMapper.findAllWithLatestPrice();
    }

    /**
     * StockPriceHistory(DB 종가) → StockPriceSnapshot 변환
     */
    private StockPriceSnapshot toSnapshotFromHistory(StockPriceHistory history) {
        BigDecimal changePercent = BigDecimal.ZERO;
        if (history.getOpenPrice() != null && history.getOpenPrice().compareTo(BigDecimal.ZERO) != 0) {
            changePercent = history.getClosePrice()
                    .subtract(history.getOpenPrice())
                    .divide(history.getOpenPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return StockPriceSnapshot.builder()
                .stockCd(history.getStockCd())
                .currentPrice(history.getClosePrice())
                .openPrice(history.getOpenPrice())
                .highPrice(history.getHighPrice())
                .lowPrice(history.getLowPrice())
                .volume(history.getVolume())
                .previousClose(history.getClosePrice())
                .changePercent(changePercent)
                .updatedAt(history.getRegDt() != null ? history.getRegDt() : LocalDateTime.now())
                .build();
    }
}

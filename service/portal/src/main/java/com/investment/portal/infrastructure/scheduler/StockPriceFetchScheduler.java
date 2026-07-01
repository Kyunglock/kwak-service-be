package com.investment.portal.infrastructure.scheduler;

import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import com.investment.portal.application.service.stock.Sp500StockListProvider;
import com.investment.portal.application.service.stock.StockPriceQueryService;
import com.investment.portal.infrastructure.cache.StockPriceCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DB 주기적 조회 → 캐시 갱신 스케줄러
 *
 * 대상: US S&P 500 종목
 * 스케줄: 2분마다 tbl_stock_price_history 최근 종가 로드
 *
 * 흐름:
 * 1. DB에서 종목별 최근 종가 일괄 조회
 * 2. 캐시 업데이트 (StockPriceCacheStore) — REST 가격 조회용
 *
 * (실시간 SSE 푸시 로직은 제거됨)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceFetchScheduler {

    private final StockPriceQueryService queryService;
    private final StockPriceCacheStore cacheStore;
    private final Sp500StockListProvider stockListProvider;

    /**
     * 2분마다 DB 최근 종가 조회 → 캐시 갱신
     */
    @Scheduled(fixedRate = 120_000) // 2분 (120초)
    public void fetchAndCache() {
        try {
            List<String> stockCodes = stockListProvider.getStockCodes();
            List<StockPriceSnapshot> snapshots = queryService.getPricesFromDb(stockCodes);

            // 캐시 갱신
            for (StockPriceSnapshot snapshot : snapshots) {
                cacheStore.put(snapshot.getStockCd(), snapshot);
            }

            log.info("[Scheduler] {}종목 DB 종가 캐시 갱신 완료", snapshots.size());

        } catch (Exception e) {
            log.error("[Scheduler] 주가 조회/갱신 실패: {}", e.getMessage(), e);
        }
    }
}

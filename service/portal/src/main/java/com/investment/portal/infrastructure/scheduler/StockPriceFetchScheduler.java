package com.investment.portal.infrastructure.scheduler;

import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import com.investment.portal.application.service.stock.Sp500StockListProvider;
import com.investment.portal.application.service.stock.StockPriceQueryService;
import com.investment.portal.infrastructure.cache.StockPriceCacheStore;
import com.investment.portal.infrastructure.sse.StockPriceSseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DB 주기적 조회 → 캐시 갱신 → SSE push 스케줄러
 *
 * 대상: US S&P 500 종목
 * 스케줄: 2분마다 tbl_stock_price_history 최근 종가 로드
 *
 * 흐름:
 * 1. DB에서 종목별 최근 종가 일괄 조회
 * 2. 캐시 업데이트 (StockPriceCacheStore)
 * 3. SSE 구독자에게 push (StockPriceSseEmitterService)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceFetchScheduler {

    private final StockPriceQueryService queryService;
    private final StockPriceCacheStore cacheStore;
    private final StockPriceSseEmitterService sseService;
    private final Sp500StockListProvider stockListProvider;

    /**
     * 2분마다 DB 최근 종가 조회 → 캐시 갱신 → SSE push
     */
    @Scheduled(fixedRate = 120_000) // 2분 (120초)
    public void fetchAndBroadcast() {
        try {
            List<String> stockCodes = stockListProvider.getStockCodes();
            List<StockPriceSnapshot> snapshots = queryService.getPricesFromDb(stockCodes);

            // 캐시 갱신
            for (StockPriceSnapshot snapshot : snapshots) {
                cacheStore.put(snapshot.getStockCd(), snapshot);
            }

            // SSE 구독자에게 push
            sseService.broadcastAll(snapshots);

            log.info("[Scheduler] {}종목 DB 종가 갱신 완료, 구독자 {}명",
                    snapshots.size(), sseService.getSubscriberCount());

        } catch (Exception e) {
            log.error("[Scheduler] 주가 조회/갱신 실패: {}", e.getMessage(), e);
        }
    }
}

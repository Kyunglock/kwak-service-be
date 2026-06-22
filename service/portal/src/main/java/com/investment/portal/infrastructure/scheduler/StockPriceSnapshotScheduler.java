package com.investment.portal.infrastructure.scheduler;

import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import com.investment.portal.domain.entity.history.stockPrice.StockPriceHistory;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import com.investment.portal.infrastructure.cache.StockPriceCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * US 장 종료 시점에 캐시 데이터를 DB(StockPriceHistory)에 저장하는 스케줄러
 *
 * - US 장 종료 16:00 ET (한국시간 06:00 KST+1) → 종가 스냅샷 DB 저장 → 캐시 초기화
 * - 대상: S&P 500 종목
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceSnapshotScheduler {

    private final StockPriceCacheStore cacheStore;
    private final StockPriceHistoryMapper stockPriceHistoryMapper;

    /**
     * US 장 종료 시점 (16:00 ET = 06:00 KST+1일)
     * 종가 스냅샷을 DB에 저장하고 캐시 초기화
     *
     * cron: 화~토 06:00 KST (= 월~금 16:00 ET)
     */
    @Scheduled(cron = "0 0 6 * * TUE-SAT", zone = "Asia/Seoul")
    public void saveClosingSnapshot() {
        log.info("[SnapshotScheduler] US 장 종료 스냅샷 저장 시작");

        ConcurrentMap<String, StockPriceSnapshot> allPrices = cacheStore.getAllAsMap();
        if (allPrices.isEmpty()) {
            log.warn("[SnapshotScheduler] 캐시에 저장된 가격 데이터가 없습니다");
            return;
        }

        List<StockPriceHistory> histories = allPrices.values().stream()
                .map(this::toStockPriceHistory)
                .toList();

        stockPriceHistoryMapper.batchInsert(histories);

        log.info("[SnapshotScheduler] US 장 종료 스냅샷 저장 완료: {}건", histories.size());

        // 장 종료 후 캐시 초기화
        cacheStore.clear();
    }

    private StockPriceHistory toStockPriceHistory(StockPriceSnapshot snapshot) {
        return StockPriceHistory.builder()
                .stockCd(snapshot.getStockCd())
                .priceDt(LocalDate.now())
                .openPrice(snapshot.getOpenPrice())
                .highPrice(snapshot.getHighPrice())
                .lowPrice(snapshot.getLowPrice())
                .closePrice(snapshot.getCurrentPrice())
                .volume(snapshot.getVolume())
                .regDt(LocalDateTime.now())
                .build();
    }
}

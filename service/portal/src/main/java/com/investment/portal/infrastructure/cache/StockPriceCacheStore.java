package com.investment.portal.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 실시간 주가 인메모리 캐시 저장소
 *
 * - Caffeine 캐시 기반: 장 마감 후 자동 만료 (6시간 TTL)
 * - 외부 API에서 받은 가격 데이터를 메모리에 보관
 * - DB에는 장 시작/종료 시점에만 스냅샷 저장
 */
@Slf4j
@Component
public class StockPriceCacheStore {

    private final Cache<String, StockPriceSnapshot> priceCache;

    public StockPriceCacheStore() {
        this.priceCache = Caffeine.newBuilder()
                .maximumSize(5_000)                  // 최대 5,000 종목
                .expireAfterWrite(Duration.ofHours(6)) // 장 마감 후 자동 만료
                .recordStats()
                .build();
    }

    /**
     * 단일 종목 가격 업데이트
     */
    public void put(String stockCd, StockPriceSnapshot snapshot) {
        priceCache.put(stockCd, snapshot);
    }

    /**
     * 다건 종목 가격 일괄 업데이트
     */
    public void putAll(Map<String, StockPriceSnapshot> snapshots) {
        priceCache.putAll(snapshots);
    }

    /**
     * 단일 종목 가격 조회
     */
    public StockPriceSnapshot get(String stockCd) {
        return priceCache.getIfPresent(stockCd);
    }

    /**
     * 캐시에 보관 중인 전체 가격 조회
     */
    public Collection<StockPriceSnapshot> getAll() {
        return priceCache.asMap().values();
    }

    /**
     * 캐시 전체 데이터를 Map으로 반환 (스냅샷 저장 용도)
     */
    public ConcurrentMap<String, StockPriceSnapshot> getAllAsMap() {
        return priceCache.asMap();
    }

    /**
     * 캐시 전체 초기화
     */
    public void clear() {
        priceCache.invalidateAll();
        log.info("[StockPriceCache] 캐시 전체 초기화 완료");
    }

    /**
     * 캐시 크기 조회
     */
    public long size() {
        return priceCache.estimatedSize();
    }
}

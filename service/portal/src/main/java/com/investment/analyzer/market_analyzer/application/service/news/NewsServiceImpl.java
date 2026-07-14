package com.investment.analyzer.market_analyzer.application.service.news;

import com.investment.analyzer.market_analyzer.application.dto.news.MarketBriefingResponse;
import com.investment.analyzer.market_analyzer.domain.repository.news.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final NewsMapper newsMapper;

    // 하루 1회(06:00 collector) 갱신 데이터 — 날짜 키 대신 짧은 TTL 전역 캐시로
    // 수동 트리거에 의한 당일 재갱신도 최대 10분 지연으로 흡수한다.
    // "요약 없음(null)"도 캐시해 매 요청 DB 조회를 막는다.
    private volatile CacheEntry cache;

    private record CacheEntry(MarketBriefingResponse value, Instant expiresAt) {}

    @Override
    public MarketBriefingResponse getMarketBriefing() {
        CacheEntry cached = cache;
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.value();
        }
        MarketBriefingResponse fresh = load();
        cache = new CacheEntry(fresh, Instant.now().plus(CACHE_TTL));
        return fresh;
    }

    private MarketBriefingResponse load() {
        return newsMapper.findLatestSummary()
                .map(s -> MarketBriefingResponse.of(
                        s, newsMapper.findArticlesForBriefing(s.getSummaryDt())))
                .orElse(null);
    }
}

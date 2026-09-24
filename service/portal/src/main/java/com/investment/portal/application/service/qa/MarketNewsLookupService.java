package com.investment.portal.application.service.qa;

import com.investment.analyzer.market_analyzer.domain.repository.news.NewsMapper;
import com.investment.portal.application.dto.qa.MarketNewsRow;
import com.investment.portal.domain.repository.stock.StockResolveMapper;
import kwak.common.collector.CollectorGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 특정 종목·날짜의 뉴스를 찾는다. DB 키워드 매칭 우선 → 없으면 collector 온디맨드
 * 크롤링. 둘 다 없어도 예외를 던지지 않고 빈 목록을 돌려준다 — 뉴스가 없어도
 * 가격 사실 답변은 계속돼야 한다(MarketAnswerPrompt가 이 경우를 처리).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketNewsLookupService {

    private static final Pattern KR_TICKER = Pattern.compile("^\\d{6}\\.(KS|KQ|KX)$");

    private final NewsMapper newsMapper;
    private final CollectorGatewayClient collectorGatewayClient;
    private final StockResolveMapper stockResolveMapper;

    public List<MarketNewsRow> findNewsForDate(String stockCd, String stockNm, LocalDate date) {
        String koreanName = stockResolveMapper.findKoreanNameByTicker(stockCd).orElse(null);
        List<String> keywords = NewsKeywords.build(stockNm, stockCd, koreanName);

        if (!keywords.isEmpty()) {
            List<MarketNewsRow> dbHits = newsMapper.findByDateAndKeyword(date, closeAtKst(stockCd, date), keywords);
            if (!dbHits.isEmpty()) {
                return dbHits;
            }
        }

        try {
            CollectorGatewayClient.CrawlResult result =
                    collectorGatewayClient.crawlOnDemand(NewsKeywords.shortName(stockNm), stockCd, date);
            if (!result.found()) {
                return List.of();
            }
            return result.articles().stream()
                    .map(a -> MarketNewsRow.builder()
                            .title(a.title())
                            .content(a.snippet())
                            .source(a.source())
                            .url(a.url())
                            .publishedAt(parsePublishedAt(a.publishedAt()))
                            .build())
                    .toList();
        } catch (Exception e) {
            // collector 장애(타임아웃 포함)가 가격 답변까지 막으면 안 된다.
            log.warn("[MarketQuestion] 온디맨드 크롤링 실패 ({}, {}): {}", stockNm, date, e.getMessage());
            return List.of();
        }
    }

    /** 해당 거래일 장 마감 시각(KST). 국내는 당일 15:30, 미국은 16:00 ET = 다음날 05:00(서머타임)~06:00 KST. */
    static LocalDateTime closeAtKst(String stockCd, LocalDate date) {
        return KR_TICKER.matcher(stockCd).matches()
                ? date.atTime(15, 30)
                : date.plusDays(1).atTime(5, 0);
    }

    /** collector 가 주는 ISO-8601(UTC, "...Z") 문자열을 화면 표시용으로만 느슨하게 변환.
     * 이 값은 이후 로직에서 날짜 비교에 쓰이지 않는다 — 대상일은 이미 확정돼 있다. */
    private LocalDateTime parsePublishedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.replace("Z", ""));
        } catch (Exception e) {
            return null;
        }
    }
}

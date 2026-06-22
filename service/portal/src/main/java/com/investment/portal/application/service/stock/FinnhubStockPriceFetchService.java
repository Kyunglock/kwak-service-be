package com.investment.portal.application.service.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Finnhub API 클라이언트
 *
 * 무료 티어: 60회/분, 배치 미지원 (개별 호출)
 * https://finnhub.io/api/v1/quote?symbol=AAPL&token=KEY
 *
 * 50종목 기준 약 55초 소요 (1.1초 간격 rate limit 보호)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinnhubStockPriceFetchService implements StockPriceFetchService {

    private final WebClient webClient;

    @Value("${stock.api.finnhub.base-url:https://finnhub.io/api/v1}")
    private String baseUrl;

    @Value("${stock.api.finnhub.api-key:}")
    private String apiKey;

    @Override
    public StockPriceSnapshot fetchPrice(String stockCd) {
        if (apiKey.isBlank()) {
            log.warn("[Finnhub] API key가 설정되지 않았습니다");
            return null;
        }

        String url = baseUrl + "/quote?symbol=" + stockCd + "&token=" + apiKey;

        try {
            FinnhubQuoteResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(FinnhubQuoteResponse.class)
                    .block();

            if (response == null || response.c == null || response.c.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("[Finnhub] 종목 {} 가격 데이터 없음", stockCd);
                return null;
            }

            return toSnapshot(stockCd, response);

        } catch (Exception e) {
            log.error("[Finnhub] 종목 {} 조회 실패: {}", stockCd, e.getMessage());
            return null;
        }
    }

    @Override
    public List<StockPriceSnapshot> fetchPrices(List<String> stockCodes) {
        if (stockCodes.isEmpty() || apiKey.isBlank()) {
            return Collections.emptyList();
        }

        // Finnhub은 배치를 지원하지 않으므로 개별 호출
        // Rate limit (60/min) 준수를 위해 순차 호출
        List<StockPriceSnapshot> results = new ArrayList<>();

        for (String stockCd : stockCodes) {
            StockPriceSnapshot snapshot = fetchPrice(stockCd);
            if (snapshot != null) {
                results.add(snapshot);
            }

            // Rate limit 보호: 60 req/min → 최소 1초 간격
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return results;
    }

    private StockPriceSnapshot toSnapshot(String stockCd, FinnhubQuoteResponse r) {
        BigDecimal changePercent = BigDecimal.ZERO;
        if (r.pc != null && r.pc.compareTo(BigDecimal.ZERO) != 0) {
            changePercent = r.dp != null ? r.dp : BigDecimal.ZERO;
        }

        return StockPriceSnapshot.builder()
                .stockCd(stockCd)
                .currentPrice(r.c)
                .openPrice(r.o)
                .highPrice(r.h)
                .lowPrice(r.l)
                .volume(null) // Finnhub quote에는 volume 없음
                .previousClose(r.pc)
                .changePercent(changePercent)
                .updatedAt(r.t != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(r.t), ZoneId.of("America/New_York"))
                        : LocalDateTime.now())
                .build();
    }

    /**
     * Finnhub /quote 응답 매핑
     * c: current, d: change, dp: change%, h: high, l: low, o: open, pc: prev close, t: timestamp
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FinnhubQuoteResponse {
        private BigDecimal c;   // Current price
        private BigDecimal d;   // Change
        private BigDecimal dp;  // Change percent
        private BigDecimal h;   // High
        private BigDecimal l;   // Low
        private BigDecimal o;   // Open
        private BigDecimal pc;  // Previous close
        private Long t;         // Timestamp
    }
}

package com.investment.portal.infrastructure.external.yahoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Yahoo Finance 비공식 API 클라이언트
 * 2024+ 부터 crumb + 쿠키 인증 필요: fc.yahoo.com 방문 → crumb 획득 → v10 호출
 */
@Slf4j
@Component
public class YahooFinanceClient {

    private static final String BASE_URL      = "https://query1.finance.yahoo.com";
    private static final String CRUMB_URL     = "https://query1.finance.yahoo.com/v1/test/getcrumb";
    private static final String MODULES       = "price,assetProfile,summaryDetail";
    private static final Duration TIMEOUT     = Duration.ofSeconds(15);
    private static final long CALL_DELAY_MS   = 400;
    private static final long CRUMB_TTL_MS    = 25 * 60 * 1000L; // 25분마다 재발급

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final WebClient    webClient;
    private final ObjectMapper objectMapper;

    private volatile String crumb;
    private volatile String cookieHeader;
    private volatile long   crumbRefreshedAt = 0L;

    public YahooFinanceClient(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.webClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json,text/html,*/*")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                .build();
        this.objectMapper = objectMapper;
    }

    // ── crumb 인증 ────────────────────────────────────────────────────────────────

    private synchronized void initCrumb() {
        long now = System.currentTimeMillis();
        // crumb이 유효하고 TTL 이내라면 재발급 생략
        if (crumb != null && (now - crumbRefreshedAt) < CRUMB_TTL_MS) return;

        try {
            WebClient plain = WebClient.builder()
                    .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                    .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,*/*")
                    .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                    .build();

            // Step 1: fc.yahoo.com 방문으로 쿠키 획득
            AtomicReference<String> cookieRef = new AtomicReference<>("");
            plain.get().uri("https://fc.yahoo.com")
                    .exchangeToMono(response -> {
                        String cookies = response.headers().asHttpHeaders()
                                .getOrDefault(HttpHeaders.SET_COOKIE, List.of())
                                .stream()
                                .map(c -> c.split(";")[0])
                                .collect(Collectors.joining("; "));
                        cookieRef.set(cookies);
                        return response.bodyToMono(Void.class).onErrorReturn(null);
                    })
                    .block(Duration.ofSeconds(10));

            String cookies = cookieRef.get();
            log.info("[YahooFinance] 쿠키 획득: {}", cookies.isEmpty() ? "(empty)" : "OK");

            // Step 2: crumb 획득
            String fetchedCrumb = plain.get()
                    .uri(CRUMB_URL)
                    .header(HttpHeaders.COOKIE, cookies.isEmpty() ? "A1=dummy" : cookies)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            if (fetchedCrumb != null && !fetchedCrumb.isBlank()
                    && !fetchedCrumb.startsWith("<") && !fetchedCrumb.contains("Unauthorized")) {
                this.crumb = fetchedCrumb.trim();
                this.cookieHeader = cookies;
                this.crumbRefreshedAt = System.currentTimeMillis();
                log.info("[YahooFinance] crumb 초기화 성공: {}", crumb);
            } else {
                log.warn("[YahooFinance] crumb 획득 실패 - response: {}",
                        fetchedCrumb != null ? fetchedCrumb.substring(0, Math.min(100, fetchedCrumb.length())) : "null");
            }
        } catch (Exception e) {
            log.warn("[YahooFinance] crumb 초기화 실패: {}", e.getMessage());
        }
    }

    private void resetCrumb() {
        synchronized (this) {
            this.crumb = null;
            this.cookieHeader = null;
            this.crumbRefreshedAt = 0L;
        }
    }

    // ── 단건 조회 ────────────────────────────────────────────────────────────────

    public Optional<StockInfo> fetchStockInfo(String ticker) {
        initCrumb();
        Optional<StockInfo> result = doFetch(ticker);

        // Unauthorized 응답이면 crumb 재발급 후 1회 재시도
        if (result.isEmpty()) {
            log.info("[YahooFinance] crumb 재발급 후 재시도 - ticker: {}", ticker);
            resetCrumb();
            initCrumb();
            result = doFetch(ticker);
        }
        return result;
    }

    private Optional<StockInfo> doFetch(String ticker) {
        try {
            final String c  = this.crumb;
            final String ck = this.cookieHeader;

            String json = webClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder
                                .path("/v10/finance/quoteSummary/{ticker}")
                                .queryParam("modules", MODULES);
                        if (c != null) b.queryParam("crumb", c);
                        return b.build(ticker.toUpperCase());
                    })
                    .headers(h -> {
                        if (ck != null && !ck.isBlank()) h.add(HttpHeaders.COOKIE, ck);
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);

            return parse(ticker, json);
        } catch (WebClientResponseException e) {
            log.warn("[YahooFinance] HTTP {} - ticker: {}", e.getStatusCode().value(), ticker);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("[YahooFinance] 조회 실패 - ticker: {}, error: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    // ── 복수 종목 일괄 조회 ──────────────────────────────────────────────────

    public Map<String, StockInfo> fetchBatch(List<String> tickers) {
        Map<String, StockInfo> result = new LinkedHashMap<>();
        for (String ticker : tickers) {
            fetchStockInfo(ticker).ifPresent(info -> result.put(ticker.toUpperCase(), info));
            try {
                Thread.sleep(CALL_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return result;
    }

    // ── JSON 파싱 ────────────────────────────────────────────────────────────────

    private Optional<StockInfo> parse(String ticker, String json) {
        if (json == null) return Optional.empty();
        try {
            JsonNode root      = objectMapper.readTree(json);
            JsonNode resultArr = root.path("quoteSummary").path("result");
            if (!resultArr.isArray() || resultArr.isEmpty()) {
                JsonNode error = root.path("quoteSummary").path("error");
                log.warn("[YahooFinance] 결과 없음 - ticker: {}, error: {}", ticker,
                        error.isMissingNode() ? "(none)" : error.toString());
                return Optional.empty();
            }

            JsonNode node    = resultArr.get(0);
            JsonNode price   = node.path("price");
            JsonNode profile = node.path("assetProfile");
            JsonNode summary = node.path("summaryDetail");

            return Optional.of(new StockInfo(
                    ticker.toUpperCase(),
                    price.path("shortName").asText(""),
                    profile.path("sector").asText(""),
                    profile.path("industry").asText(""),
                    raw(price,   "regularMarketPrice"),
                    raw(price,   "regularMarketChangePercent"),
                    (long) raw(price, "marketCap"),
                    raw(summary, "trailingPE"),
                    raw(summary, "dividendYield"),
                    raw(summary, "fiftyTwoWeekLow"),
                    raw(summary, "fiftyTwoWeekHigh"),
                    price.path("currency").asText("USD")
            ));
        } catch (Exception e) {
            log.warn("[YahooFinance] 파싱 실패 - ticker: {}", ticker, e);
            return Optional.empty();
        }
    }

    /** Yahoo Finance는 숫자값을 {"raw": 123, "fmt": "123"} 구조로 감싸서 반환 */
    private double raw(JsonNode parent, String field) {
        return parent.path(field).path("raw").asDouble(0);
    }
}

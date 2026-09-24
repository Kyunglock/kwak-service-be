package kwak.common.collector;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * portal → collector(:8000) 내부 API 호출 클라이언트. X-System-Key 로 인증.
 *
 * <p>{@link kwak.common.ai.AiGatewayClient}와 대칭 위치·구조다. collector 는 원래
 * n8n이 {@code COLLECTOR_URI}로 호출하던 대상인데, 이 클라이언트가 portal에서
 * collector로 가는 첫 직접 호출 경로다 — 지금까지 portal의 "collector" 언급은
 * 전부 "collector가 적재한 DB"를 가리켰을 뿐, HTTP 호출은 없었다.
 */
@Component
public class CollectorGatewayClient {

    /** 온디맨드 크롤링으로 찾은 기사 한 건. */
    public record CrawledArticle(String title, String source, String url, String snippet, String publishedAt) {}

    /** 온디맨드 크롤링 결과. found=false 면 articles 는 빈 목록. */
    public record CrawlResult(boolean found, List<CrawledArticle> articles, int savedCount) {}

    // 헤드리스 브라우저로 사이트 두 곳(Google News → Yahoo Finance)을 순서대로
    // 시도하므로 단발 API 호출보다 느리다. collector 쪽 사이트당 상한(15초) x 2에
    // 브라우저 기동 오버헤드를 더해 여유 있게 둔다.
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final WebClient webClient;
    private final String systemKey;

    public CollectorGatewayClient(
            @Value("${collector.base-url:http://localhost:8000}") String baseUrl,
            @Value("${system.api-key:}") String systemKey) {
        this.systemKey = systemKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * 특정 종목·날짜의 뉴스를 collector가 실시간으로 찾아오게 한다. collector가
     * 찾은 기사는 그쪽에서 이미 DB에 저장한 뒤, 같은 응답으로 내용을 돌려준다.
     *
     * @throws RuntimeException collector 호출 자체가 실패한 경우 (타임아웃 포함).
     *     호출부(MarketNewsLookupService)가 이를 "뉴스 없음"과 동일하게 흡수한다 —
     *     크롤러 장애가 가격 답변까지 막으면 안 된다.
     */
    @SuppressWarnings("unchecked")
    public CrawlResult crawlOnDemand(String stockName, String ticker, LocalDate targetDate) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("stockName", stockName);
        body.put("ticker", ticker);
        body.put("targetDate", targetDate.toString());

        Map<String, Object> res = webClient.post()
                .uri("/internal/news/crawl-on-demand")
                .header("X-System-Key", systemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block(TIMEOUT);

        if (res == null) {
            return new CrawlResult(false, List.of(), 0);
        }

        List<Map<String, Object>> rawArticles = (List<Map<String, Object>>) res.getOrDefault("articles", List.of());
        List<CrawledArticle> articles = rawArticles.stream()
                .map(a -> new CrawledArticle(
                        String.valueOf(a.get("title")),
                        String.valueOf(a.get("source")),
                        String.valueOf(a.get("url")),
                        a.get("snippet") == null ? null : String.valueOf(a.get("snippet")),
                        a.get("publishedAt") == null ? null : String.valueOf(a.get("publishedAt"))))
                .toList();

        boolean found = Boolean.TRUE.equals(res.get("found"));
        int savedCount = res.get("savedCount") == null ? 0 : ((Number) res.get("savedCount")).intValue();
        return new CrawlResult(found, articles, savedCount);
    }
}

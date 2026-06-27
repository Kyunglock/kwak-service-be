package com.investment.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 검증 GlobalFilter (Spring Cloud Gateway / WebFlux)
 *
 * 처리 순서:
 * 1. OPTIONS 사전 요청 → 통과
 * 2. 공개 경로 → 통과
 * 3. JWT 추출 (Authorization 헤더 또는 accessToken 쿠키)
 * 4. JWT 서명 검증
 *    - 만료(ExpiredJwtException) → refreshToken 쿠키로 포털 /auth/refresh 호출
 *    - 기타 오류 → 401
 * 5. Redis 블랙리스트 확인
 * 6. 통과 시 X-User-Session-Id 헤더를 하위 서비스에 전달
 */
@Slf4j
@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String SESSION_ID_HEADER = "X-User-Session-Id";

    // StripPrefix 필터는 라우팅 이후에 적용되므로, JWT 필터가 보는 경로는
    // 서비스 prefix(/portal, /survey 등)가 붙어 있는 원본 경로다.
    private static final List<String> PUBLIC_PATHS = List.of(
            // prefix 없는 경로 (직접 접근 또는 내부 호출)
            "/api/v1/auth/**",
            "/api/v1/stocks/price/**",
            "/api/v1/kwakai/**",
            // 게이트웨이 prefix 포함 경로
            "/portal/api/v1/auth/**",
            "/portal/api/v1/stocks/price/**",
            "/portal/api/v1/kwakai/**",
            "/survey/api/v1/auth/**",
            "/advisor/api/v1/auth/**",
            "/market/api/v1/auth/**"
    );

    private final SecretKey secretKey;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient portalWebClient;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtGlobalFilter(
            @Value("${jwt.secret}") String secret,
            @Value("${PORTAL_URI:http://localhost:8080}") String portalUri,
            ReactiveStringRedisTemplate redisTemplate,
            WebClient.Builder webClientBuilder) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.redisTemplate = redisTemplate;
        this.portalWebClient = webClientBuilder.baseUrl(portalUri).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        if (isPublicPath(path)) {
            log.debug("[JwtGlobalFilter] 공개 경로 통과 - path: {}", path);
            return chain.filter(exchange);
        }

        String token = resolveToken(request);
        if (token == null) {
            log.warn("[JwtGlobalFilter] 토큰 없음 - path: {}", path);
            return unauthorized(exchange);
        }

        Claims claims;
        try {
            claims = parseClaims(token);
        } catch (ExpiredJwtException e) {
            log.debug("[JwtGlobalFilter] 액세스 토큰 만료 - 리프레시 시도, path: {}", path);
            return tryRefresh(exchange, chain, path);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JwtGlobalFilter] JWT 검증 실패 - path: {}, error: {}", path, e.getMessage());
            return unauthorized(exchange);
        }

        // 블랙리스트 확인 (로그아웃된 토큰 차단)
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        log.warn("[JwtGlobalFilter] 블랙리스트 토큰 - sessionId: {}", claims.getSubject());
                        return unauthorized(exchange);
                    }
                    return proceed(exchange, chain, claims.getSubject());
                });
    }

    /**
     * 액세스 토큰 만료 시 리프레시 토큰으로 포털에 새 액세스 토큰 요청
     */
    private Mono<Void> tryRefresh(ServerWebExchange exchange, GatewayFilterChain chain, String path) {
        String refreshToken = getCookieValue(exchange.getRequest(), "refreshToken");
        if (refreshToken == null) {
            log.warn("[JwtGlobalFilter] 리프레시 토큰 없음 - path: {}", path);
            return unauthorized(exchange);
        }

        return portalWebClient.post()
                .uri("/api/v1/auth/refresh")
                .cookie("refreshToken", refreshToken)
                .exchangeToMono(portalResponse -> {
                    // 포털이 Set-Cookie(accessToken)를 내려주면 클라이언트에 그대로 전달
                    List<String> setCookieHeaders = portalResponse.headers().header(HttpHeaders.SET_COOKIE);
                    setCookieHeaders.forEach(h ->
                            exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, h)
                    );
                    return portalResponse.bodyToMono(RefreshApiResponse.class);
                })
                .flatMap(apiResponse -> {
                    if (apiResponse == null || !apiResponse.success() || apiResponse.data() == null) {
                        log.warn("[JwtGlobalFilter] 리프레시 실패 - path: {}", path);
                        return unauthorized(exchange);
                    }

                    String newToken = apiResponse.data().accessToken();
                    Claims newClaims;
                    try {
                        newClaims = parseClaims(newToken);
                    } catch (Exception e) {
                        log.warn("[JwtGlobalFilter] 갱신된 토큰 파싱 실패 - {}", e.getMessage());
                        return unauthorized(exchange);
                    }

                    log.debug("[JwtGlobalFilter] 토큰 갱신 완료 - sessionId: {}", newClaims.getSubject());
                    return proceed(exchange, chain, newClaims.getSubject());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[JwtGlobalFilter] 포털 응답 없음 - path: {}", path);
                    return unauthorized(exchange);
                }))
                .onErrorResume(e -> {
                    log.error("[JwtGlobalFilter] 리프레시 호출 오류 - {}", e.getMessage());
                    return unauthorized(exchange);
                });
    }

    /**
     * 인증 통과: X-User-Session-Id 헤더를 붙여 하위 서비스로 전달
     */
    private Mono<Void> proceed(ServerWebExchange exchange, GatewayFilterChain chain, String sessionId) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(SESSION_ID_HEADER, sessionId)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private String resolveToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }
        HttpCookie cookie = request.getCookies().getFirst("accessToken");
        return cookie != null ? cookie.getValue() : null;
    }

    private String getCookieValue(ServerHttpRequest request, String name) {
        HttpCookie cookie = request.getCookies().getFirst(name);
        return cookie != null ? cookie.getValue() : null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // 포털 응답 파싱용 내부 레코드
    private record RefreshApiResponse(boolean success, RefreshData data, String message) {}
    private record RefreshData(String accessToken) {}
}

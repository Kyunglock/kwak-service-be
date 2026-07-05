package com.investment.portal.config;

import com.investment.portal.domain.entity.log.ApiLog;
import com.investment.portal.domain.repository.log.ApiLogMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * API 요청 로깅 필터 — 모든 /api/** 요청의 URL과 요청 데이터를 서버 로그 + tbl_api_log 에 남긴다.
 * 응답 본문은 남기지 않는다 (용량 문제).
 *
 * - 공격 시도 탐지가 목적이므로 Security 필터(-100)보다 앞(@Order(-200))에서 실행 —
 *   인증 실패(401/403)로 차단된 요청도 상태코드와 함께 기록된다.
 * - 인증된 요청의 userId 는 ApiLogUserIdCapture 인터셉터가 request attribute 로 전달한다.
 * - body 는 스트림이라 그냥 읽으면 컨트롤러가 못 읽으므로 ContentCachingRequestWrapper 로 캐싱,
 *   처리 완료 후(finally) 캐시에서 꺼내 기록한다.
 * - 인증 관련 경로(/api/v1/auth/**)는 비밀번호·토큰이 담기므로 body 를 마스킹한다.
 * - 로그 적재 실패가 본 요청을 깨뜨리지 않도록 예외를 삼킨다.
 */
@Slf4j
@Component
@Order(-200)
@RequiredArgsConstructor
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_PAYLOAD_LENGTH = 1000;
    private static final String MASKED_PATH_PREFIX = "/api/v1/auth";

    private final ApiLogMapper apiLogMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // API 요청만 대상. CORS preflight(OPTIONS)는 제외.
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            logRequest(wrapped, response.getStatus());
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, int status) {
        String query = request.getQueryString();
        String url = request.getRequestURI() + (query != null ? "?" + query : "");
        String body = extractBody(request);
        String userId = (String) request.getAttribute(ApiLogUserIdCapture.USER_ID_ATTR);

        if (body.isEmpty()) {
            log.info("[API] {} {} ({})", request.getMethod(), url, status);
        } else {
            log.info("[API] {} {} ({}) body={}", request.getMethod(), url, status, body);
        }

        try {
            apiLogMapper.insert(ApiLog.builder()
                    .userId(userId)
                    .ip(clientIp(request))
                    .method(request.getMethod())
                    .url(truncate(url, 500))
                    .requestBody(body.isEmpty() ? null : body)
                    .status(status)
                    .userAgent(truncate(request.getHeader("User-Agent"), 255))
                    .build());
        } catch (Exception e) {
            log.warn("[API] tbl_api_log 적재 실패 - url: {}, err: {}", url, e.getMessage());
        }
    }

    private String extractBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) return "";
        if (request.getRequestURI().startsWith(MASKED_PATH_PREFIX)) return "(masked)";

        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/")) return "(multipart)";

        Charset charset = request.getCharacterEncoding() != null
                ? Charset.forName(request.getCharacterEncoding()) : StandardCharsets.UTF_8;
        String body = new String(content, charset).replaceAll("\\s*\\R\\s*", " ");
        return body.length() <= MAX_PAYLOAD_LENGTH
                ? body : body.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
    }

    /** 프록시(nginx) 뒤에서는 X-Forwarded-For 첫 번째 값이 실제 클라이언트 IP */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}

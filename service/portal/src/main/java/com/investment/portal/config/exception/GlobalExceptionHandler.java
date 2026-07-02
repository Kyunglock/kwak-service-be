package com.investment.portal.config.exception;

import kwak.common.exception.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${app.frontend.url:http://192.168.0.8:5173}")
    private String frontendUrl;

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e, HttpServletRequest request) {
        log.debug("SSE 클라이언트 연결 종료 - URI: {}", request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {

        log.error("인증 오류 - URI: {}, 코드: {}, 메시지: {}",
            request.getRequestURI(), e.getErrorCode(), e.getMessage(), e);

        if (request.getRequestURI().contains("/callback")) {
            String url = String.format("%s/login?error=%s",
                frontendUrl,
                URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));

            log.info("로그인 페이지로 리다이렉트: {}", url);

            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
        }

        return ResponseEntity.status(e.getHttpStatus())
            .body(Map.of("success", false, "errorCode", e.getErrorCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        log.error("예상치 못한 오류 - URI: {}", request.getRequestURI(), e);

        if (request.getRequestURI().contains("/callback")) {
            String url = String.format("%s/login?error=%s",
                frontendUrl,
                URLEncoder.encode("로그인 처리 중 오류가 발생했습니다.", StandardCharsets.UTF_8));

            log.info("로그인 페이지로 리다이렉트: {}", url);

            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("success", false, "errorCode", "INTERNAL_SERVER_ERROR",
                "message", e.getMessage() != null ? e.getMessage() : "서버 오류"));
    }
}

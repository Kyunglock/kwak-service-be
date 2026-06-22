package com.investment.portal.config.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Controller
public class CustomErrorController implements ErrorController {
    
    @Value("${app.frontend.url:http://192.168.0.8:5173}")
    private String frontendUrl;
    
    @RequestMapping("/error")
    public ResponseEntity<?> handleError(HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        Throwable throwable = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        String uri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        
        if (status == null) status = 500;
        if (message == null || message.isEmpty()) {
            message = throwable != null ? throwable.getMessage() : "오류가 발생했습니다.";
            if (message == null) message = "오류가 발생했습니다.";
        }
        
        log.error("CustomErrorController - Status: {}, URI: {}, Message: {}", status, uri, message, throwable);
        
        // 카카오 콜백이면 로그인 페이지로 리다이렉트
        boolean isCallback = uri != null && uri.contains("/callback");
        String accept = request.getHeader("Accept");
        boolean isBrowser = accept != null && accept.contains("text/html");
        
        if (isCallback || isBrowser) {
            String redirectUrl = String.format("%s/login?error=%s",
                frontendUrl, URLEncoder.encode(message, StandardCharsets.UTF_8));
            
            log.info("로그인 페이지로 리다이렉트: {}", redirectUrl);
            
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
        }
        
        // API 요청이면 JSON
        return ResponseEntity.status(status)
            .body(Map.of(
                "success", false,
                "errorCode", "ERROR_" + status,
                "message", message
            ));
    }
}
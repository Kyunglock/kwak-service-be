package com.investment.portal.api.controller.login.standard;

import com.investment.portal.application.dto.login.standard.StandardLoginRequest;
import com.investment.portal.application.event.ActivityEvent;
import com.investment.portal.application.service.login.LoginResponse;
import com.investment.portal.application.service.login.standard.StandardAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/standard")
@RequiredArgsConstructor
@Tag(name = "Standard Auth", description = "일반 로그인 (이메일/비밀번호) API")
public class StandardAuthController {

    private final StandardAuthService standardAuthService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    @Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인")
    public ResponseEntity<?> login(
            @Valid @RequestBody StandardLoginRequest request,
            HttpServletResponse response) {

        log.info("일반 로그인 요청 - 이메일: {}", request.email());
        LoginResponse loginResponse = standardAuthService.login(request);

        // 액세스 토큰 쿠키 (1시간)
        ResponseCookie accessCookie = ResponseCookie
                .from("accessToken", loginResponse.accessToken())
                .maxAge(60 * 60)
                .path("/")
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();

        // 리프레시 토큰 쿠키 (7일)
        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", loginResponse.refreshToken())
                .maxAge(7 * 24 * 60 * 60)
                .path("/")
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        eventPublisher.publishEvent(ActivityEvent.of(
                loginResponse.userId(), "LOGIN", "AUTH", "STANDARD", "일반 로그인"));

        return ResponseUtil.success(loginResponse);
    }
}

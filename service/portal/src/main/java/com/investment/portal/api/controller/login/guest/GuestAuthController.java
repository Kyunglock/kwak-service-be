package com.investment.portal.api.controller.login.guest;

import kwak.common.application.event.ActivityEvent;
import com.investment.portal.application.service.login.LoginResponse;
import com.investment.portal.application.service.login.guest.GuestAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/v1/auth/guest")
@RequiredArgsConstructor
@Tag(name = "Guest Auth", description = "손님 로그인 API")
public class GuestAuthController {

    private final GuestAuthService guestAuthService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/login")
    @Operation(summary = "손님 로그인", description = "랜덤 ID로 손님 계정을 생성하고 로그인합니다")
    public ResponseEntity<?> guestLogin(HttpServletResponse response) {
        log.info("손님 로그인 요청");
        LoginResponse loginResponse = guestAuthService.guestLogin();

        ResponseCookie accessCookie = ResponseCookie
                .from("accessToken", loginResponse.accessToken())
                .maxAge(60 * 60)
                .path("/")
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();

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
                loginResponse.userId(), "LOGIN", "AUTH", "GUEST", "게스트 로그인"));

        return ResponseUtil.success(loginResponse);
    }
}

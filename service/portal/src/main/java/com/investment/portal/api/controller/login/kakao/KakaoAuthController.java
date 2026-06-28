package com.investment.portal.api.controller.login.kakao;

import com.investment.portal.application.service.login.LoginResponse;
import com.investment.portal.application.service.login.kakao.KakaoAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/kakao")
@RequiredArgsConstructor
@Tag(name = "Kakao OAuth", description = "카카오 소셜 로그인 API")
public class KakaoAuthController {

    final private KakaoAuthService kakaoAuthService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @GetMapping("/login")
    @Operation(summary = "카카오 로그인 페이지 리다이렉트", description = "카카오 OAuth 인증 페이지로 리다이렉트")
    public ResponseEntity<?> redirectToKakaoLogin() {
        String kakaoAuthUrl = kakaoAuthService.getKakaoAuthUrl();
        return ResponseUtil.success(kakaoAuthUrl);
    }

    @GetMapping("/callback")
    @Operation(summary = "카카오 로그인 콜백")
    public ResponseEntity<?> kakaoCallback(
            @RequestParam("code") String code,
            HttpServletResponse response) {
        
        log.info("카카오 로그인 콜백 - 인가 코드: {}", code);
        LoginResponse loginResponse = kakaoAuthService.loginWithKakao(code);
        
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

        return ResponseUtil.redirect(frontendUrl + "/oauth/callback");
    }

    @PostMapping("/unlink")
    @Operation(summary = "카카오 연결 해제", description = "사용자의 카카오 계정 연결 해제")
    public ResponseEntity<?> unlinkKakao(
            @Parameter(description = "사용자 ID")
            @RequestParam("userId") String userId) {
        
        log.info("카카오 연결 해제 요청 - 사용자 ID: {}", userId);
        kakaoAuthService.unlinkKakao(userId);
        
        return ResponseUtil.noContent();
    }
}
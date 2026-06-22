package com.investment.portal.api.controller.login;

import kwak.common.config.security.JwtTokenProvider;
import kwak.common.infrastructure.token.RedisTokenStore;
import kwak.common.infrastructure.token.UserSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 공통 API")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "JWT 토큰 무효화 및 쿠키 삭제")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = resolveToken(request, "accessToken");
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            jwtTokenProvider.invalidateToken(accessToken);
        }

        // 리프레시 토큰 Redis 삭제
        String refreshToken = resolveToken(request, "refreshToken");
        if (refreshToken != null) {
            redisTokenStore.deleteRefreshToken(refreshToken);
        }

        // accessToken 쿠키 삭제
        response.addHeader("Set-Cookie", emptyCookie("accessToken").toString());
        // refreshToken 쿠키 삭제
        response.addHeader("Set-Cookie", emptyCookie("refreshToken").toString());

        return ResponseUtil.success(null, "로그아웃 성공");
    }

    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰 발급")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenId = resolveToken(request, "refreshToken");
        if (refreshTokenId == null) {
            return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
        }

        // Redis에서 refreshTokenId → sessionId 조회
        String sessionId = redisTokenStore.getSessionIdByRefreshToken(refreshTokenId);
        if (sessionId == null) {
            log.warn("[AuthController] 유효하지 않거나 만료된 리프레시 토큰");
            return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        // 세션(UserSession) 존재 확인
        UserSession session = redisTokenStore.getSession(sessionId);
        if (session == null) {
            redisTokenStore.deleteRefreshToken(refreshTokenId);
            log.warn("[AuthController] 세션 소멸 - sessionId: {}", sessionId);
            return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해 주세요.");
        }

        // 새 액세스 토큰 발급 (기존 sessionId 재사용)
        String newAccessToken = jwtTokenProvider.createToken(sessionId);
        log.debug("[AuthController] 액세스 토큰 갱신 - sessionId: {}", sessionId);

        // 새 accessToken 쿠키 설정 (1시간)
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                .maxAge(60 * 60)
                .path("/")
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        return ResponseUtil.success(new TokenRefreshResponse(newAccessToken), "토큰이 갱신되었습니다.");
    }

    /**
     * 쿠키 또는 Authorization 헤더에서 특정 쿠키 값 추출
     */
    private String resolveToken(HttpServletRequest request, String cookieName) {
        // Authorization 헤더는 accessToken 에만 적용
        if ("accessToken".equals(cookieName)) {
            String bearer = request.getHeader("Authorization");
            if (bearer != null && bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private ResponseCookie emptyCookie(String name) {
        return ResponseCookie.from(name, "")
                .maxAge(0)
                .path("/")
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .build();
    }

    public record TokenRefreshResponse(String accessToken) {}
}

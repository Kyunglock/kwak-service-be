package com.investment.portal.api.controller.login;

import kwak.common.application.event.ActivityEvent;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    /**
     * 회전된 구 리프레시 토큰의 유예 시간.
     * 만료된 액세스 토큰을 들고 거의 동시에 들어온 요청들이 구 토큰으로 refresh를 시도해도
     * 이 시간 안에는 선점자가 만든 같은 새 토큰을 공유받는다.
     */
    @Value("${jwt.rotation-grace:60000}")
    private long rotationGraceMs;

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "JWT 토큰 무효화 및 쿠키 삭제")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal String userId,
            HttpServletRequest request, HttpServletResponse response) {
        // 만료된 액세스 토큰이어도 세션 삭제(즉시 무효화)는 수행되어야 한다
        String accessToken = resolveToken(request, "accessToken");
        if (accessToken != null) {
            jwtTokenProvider.invalidateToken(accessToken);
        }

        // 리프레시 토큰 경유로도 세션 정리 (액세스 토큰이 없거나 파싱 불가한 경우 대비)
        String refreshToken = resolveToken(request, "refreshToken");
        if (refreshToken != null) {
            String sessionId = redisTokenStore.getSessionIdByRefreshToken(refreshToken);
            if (sessionId == null) {
                // 회전 grace 구간의 구 토큰일 수 있으므로 새 토큰 경유로 재시도
                String rotatedNewId = redisTokenStore.getRotatedNewRefreshTokenId(refreshToken);
                if (rotatedNewId != null) {
                    sessionId = redisTokenStore.getSessionIdByRefreshToken(rotatedNewId);
                }
            }
            if (sessionId != null) {
                redisTokenStore.deleteSession(sessionId);
                redisTokenStore.revokeRefreshTokensBySession(sessionId);
            }
            redisTokenStore.deleteRefreshToken(refreshToken);
        }

        // accessToken 쿠키 삭제
        response.addHeader("Set-Cookie", emptyCookie("accessToken", false).toString());
        // refreshToken 쿠키 삭제
        response.addHeader("Set-Cookie", emptyCookie("refreshToken", true).toString());

        if (userId != null) {
            eventPublisher.publishEvent(ActivityEvent.of(userId, "LOGOUT", "AUTH", null, "로그아웃"));
        }

        return ResponseUtil.success(null, "로그아웃 성공");
    }

    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 갱신",
            description = "리프레시 토큰으로 새 액세스 토큰 발급. 리프레시 토큰은 매 호출마다 회전되며, "
                    + "회전된 구 토큰의 재사용이 탐지되면 탈취로 간주해 세션을 강제 종료한다.")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String presentedTokenId = resolveToken(request, "refreshToken");
        if (presentedTokenId == null) {
            return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
        }

        String sessionId = redisTokenStore.getSessionIdByRefreshToken(presentedTokenId);
        long remainingMs = sessionId != null ? redisTokenStore.getRefreshTokenTtlMs(presentedTokenId) : -1;
        String activeRefreshTokenId;

        if (sessionId != null && remainingMs > 0) {
            // 활성 토큰 → 회전. SET NX 선점으로 동시 요청 중 한 건만 새 토큰을 만들고 나머지는 공유한다.
            String candidateNewId = jwtTokenProvider.generateRefreshTokenId();
            if (redisTokenStore.tryClaimRotation(presentedTokenId, candidateNewId, rotationGraceMs)) {
                redisTokenStore.completeRotation(presentedTokenId, candidateNewId, sessionId, remainingMs);
                activeRefreshTokenId = candidateNewId;
            } else {
                activeRefreshTokenId = redisTokenStore.getRotatedNewRefreshTokenId(presentedTokenId);
                if (activeRefreshTokenId == null) {
                    log.warn("[AuthController] 회전 경합 후 새 토큰 조회 실패 - sessionId: {}", sessionId);
                    return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
                }
            }
        } else {
            // 활성 토큰이 아님: 회전 grace 구간의 동시 요청인지 먼저 확인
            String rotatedNewId = redisTokenStore.getRotatedNewRefreshTokenId(presentedTokenId);
            if (rotatedNewId != null) {
                sessionId = redisTokenStore.getSessionIdByRefreshToken(rotatedNewId);
                if (sessionId == null) {
                    return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
                }
                activeRefreshTokenId = rotatedNewId;
            } else {
                // grace가 끝난 폐기 토큰의 재사용 → 탈취 의심, 세션 전체 강제 종료
                String compromisedSessionId = redisTokenStore.getUsedRefreshTokenSessionId(presentedTokenId);
                if (compromisedSessionId != null) {
                    redisTokenStore.deleteSession(compromisedSessionId);
                    redisTokenStore.revokeRefreshTokensBySession(compromisedSessionId);
                    log.warn("[AuthController] 리프레시 토큰 재사용 탐지 - 세션 강제 종료: {}", compromisedSessionId);
                    return ResponseUtil.error(HttpStatus.UNAUTHORIZED,
                            "비정상적인 토큰 사용이 감지되어 로그아웃되었습니다. 다시 로그인해 주세요.");
                }
                log.warn("[AuthController] 유효하지 않거나 만료된 리프레시 토큰");
                return ResponseUtil.error(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
            }
        }

        // 세션(UserSession) 존재 확인
        UserSession session = redisTokenStore.getSession(sessionId);
        if (session == null) {
            redisTokenStore.deleteRefreshToken(activeRefreshTokenId);
            redisTokenStore.revokeRefreshTokensBySession(sessionId);
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
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        // 회전된 refreshToken 쿠키 설정 (7일, XSS 방어를 위해 httpOnly)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", activeRefreshTokenId)
                .maxAge(7 * 24 * 60 * 60)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

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

    private ResponseCookie emptyCookie(String name, boolean httpOnly) {
        return ResponseCookie.from(name, "")
                .maxAge(0)
                .path("/")
                .httpOnly(httpOnly)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();
    }

    public record TokenRefreshResponse(String accessToken) {}
}

package kwak.common.config.security;

import kwak.common.infrastructure.token.RedisTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long validityInMilliseconds;
    private final long refreshValidityInMilliseconds;
    private final RedisTokenStore redisTokenStore;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:3600000}") long validityInMilliseconds,
            @Value("${jwt.refresh-expiration:604800000}") long refreshValidityInMilliseconds,
            RedisTokenStore redisTokenStore) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
        this.refreshValidityInMilliseconds = refreshValidityInMilliseconds;
        this.redisTokenStore = redisTokenStore;
    }

    /**
     * JWT 토큰 생성 (sessionId만 포함, userId 미포함)
     */
    public String createToken(String sessionId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(sessionId)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 새로운 sessionId 생성
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public long getValidityInMilliseconds() {
        return validityInMilliseconds;
    }

    public long getRefreshValidityInMilliseconds() {
        return refreshValidityInMilliseconds;
    }

    public String generateRefreshTokenId() {
        return UUID.randomUUID().toString();
    }

    /**
     * JWT 토큰에서 sessionId 추출
     */
    public String getSessionId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * JWT 토큰 남은 만료시간 (ms)
     */
    public long getRemainingExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * JWT 토큰 유효성 검증 (서명 + 블랙리스트)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            if (redisTokenStore.isBlacklisted(token)) {
                log.warn("블랙리스트에 등록된 토큰입니다.");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 로그아웃 처리 (세션 삭제 + 블랙리스트 등록)
     */
    public void invalidateToken(String token) {
        String sessionId = getSessionId(token);
        long remainingMs = getRemainingExpiration(token);

        redisTokenStore.deleteSession(sessionId);
        redisTokenStore.addToBlacklist(token, remainingMs);

        log.info("[JwtTokenProvider] 토큰 무효화 - sessionId: {}", sessionId);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

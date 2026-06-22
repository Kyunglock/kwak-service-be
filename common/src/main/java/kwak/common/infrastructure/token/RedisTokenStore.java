package kwak.common.infrastructure.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_PREFIX = "auth:refresh:";

    /**
     * 유저 세션 정보 저장 (sessionId 기반)
     */
    public void saveSession(String sessionId, @NonNull UserSession session, long expirationMs) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, session, expirationMs, TimeUnit.MILLISECONDS);
        log.debug("[RedisTokenStore] 세션 저장 - sessionId: {}", sessionId);
    }

    /**
     * 유저 세션 정보 조회 (sessionId 기반)
     */
    public UserSession getSession(String sessionId) {
        Object value = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        if (value instanceof UserSession session) {
            return session;
        }
        return null;
    }

    /**
     * 세션 삭제 (로그아웃 시)
     */
    public void deleteSession(String sessionId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);
        log.debug("[RedisTokenStore] 세션 삭제 - sessionId: {}", sessionId);
    }

    /**
     * 토큰 블랙리스트 등록 (로그아웃된 토큰 무효화)
     */
    public void addToBlacklist(String token, long remainingMs) {
        if (remainingMs > 0) {
            String key = BLACKLIST_PREFIX + token;
            stringRedisTemplate.opsForValue().set(key, "logout", remainingMs, TimeUnit.MILLISECONDS);
            log.debug("[RedisTokenStore] 토큰 블랙리스트 등록");
        }
    }

    /**
     * 블랙리스트 여부 확인
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    /**
     * 리프레시 토큰 저장 (refreshTokenId → sessionId)
     */
    public void saveRefreshToken(String refreshTokenId, String sessionId, long expirationMs) {
        String key = REFRESH_PREFIX + refreshTokenId;
        stringRedisTemplate.opsForValue().set(key, sessionId, expirationMs, TimeUnit.MILLISECONDS);
        log.debug("[RedisTokenStore] 리프레시 토큰 저장 - refreshTokenId: {}", refreshTokenId);
    }

    /**
     * 리프레시 토큰으로 sessionId 조회
     */
    public String getSessionIdByRefreshToken(String refreshTokenId) {
        return stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + refreshTokenId);
    }

    /**
     * 리프레시 토큰 삭제 (로그아웃 시)
     */
    public void deleteRefreshToken(String refreshTokenId) {
        stringRedisTemplate.delete(REFRESH_PREFIX + refreshTokenId);
        log.debug("[RedisTokenStore] 리프레시 토큰 삭제 - refreshTokenId: {}", refreshTokenId);
    }
}

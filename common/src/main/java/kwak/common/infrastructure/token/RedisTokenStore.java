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
    private static final String REFRESH_ROTATED_PREFIX = "auth:refresh-rotated:";
    private static final String REFRESH_USED_PREFIX = "auth:refresh-used:";
    private static final String SESSION_REFRESH_PREFIX = "auth:session-refresh:";

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
     * 세션 → 활성 리프레시 토큰 역방향 매핑도 함께 저장한다 (세션 단위 폐기용)
     */
    public void saveRefreshToken(String refreshTokenId, String sessionId, long expirationMs) {
        stringRedisTemplate.opsForValue()
                .set(REFRESH_PREFIX + refreshTokenId, sessionId, expirationMs, TimeUnit.MILLISECONDS);
        stringRedisTemplate.opsForValue()
                .set(SESSION_REFRESH_PREFIX + sessionId, refreshTokenId, expirationMs, TimeUnit.MILLISECONDS);
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

    /**
     * 리프레시 토큰의 남은 TTL(ms). 키가 없거나 TTL이 없으면 음수를 반환한다.
     */
    public long getRefreshTokenTtlMs(String refreshTokenId) {
        Long expire = stringRedisTemplate.getExpire(REFRESH_PREFIX + refreshTokenId, TimeUnit.MILLISECONDS);
        return expire != null ? expire : -1;
    }

    /**
     * 리프레시 토큰 회전 선점 (SET NX).
     * 같은 구 토큰을 든 동시 요청 중 정확히 한 요청만 true를 받고,
     * 나머지는 {@link #getRotatedNewRefreshTokenId}로 선점자가 만든 새 토큰을 공유한다.
     */
    public boolean tryClaimRotation(String oldRefreshTokenId, String newRefreshTokenId, long graceMs) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
                REFRESH_ROTATED_PREFIX + oldRefreshTokenId, newRefreshTokenId, graceMs, TimeUnit.MILLISECONDS));
    }

    /**
     * 회전 완료: 새 토큰 저장 → 구 토큰 삭제 → 재사용 탐지 마커 등록.
     * 구 토큰 삭제 전에 새 토큰을 먼저 저장해 동시 요청이 빈틈을 보지 않게 한다.
     */
    public void completeRotation(String oldRefreshTokenId, String newRefreshTokenId,
                                 String sessionId, long remainingMs) {
        saveRefreshToken(newRefreshTokenId, sessionId, remainingMs);
        stringRedisTemplate.delete(REFRESH_PREFIX + oldRefreshTokenId);
        stringRedisTemplate.opsForValue().set(
                REFRESH_USED_PREFIX + oldRefreshTokenId, sessionId, remainingMs, TimeUnit.MILLISECONDS);
        log.debug("[RedisTokenStore] 리프레시 토큰 회전 완료 - old: {}, new: {}", oldRefreshTokenId, newRefreshTokenId);
    }

    /**
     * 회전 grace 구간의 구 토큰이 가리키는 새 토큰 조회. grace 만료 후에는 null.
     */
    public String getRotatedNewRefreshTokenId(String oldRefreshTokenId) {
        return stringRedisTemplate.opsForValue().get(REFRESH_ROTATED_PREFIX + oldRefreshTokenId);
    }

    /**
     * 폐기(회전)된 리프레시 토큰의 재사용 여부 확인. 값이 있으면 탈취 의심 상황이며 해당 sessionId를 반환한다.
     */
    public String getUsedRefreshTokenSessionId(String refreshTokenId) {
        return stringRedisTemplate.opsForValue().get(REFRESH_USED_PREFIX + refreshTokenId);
    }

    /**
     * 세션의 활성 리프레시 토큰 일괄 폐기 (로그아웃, 재사용 탐지 시)
     */
    public void revokeRefreshTokensBySession(String sessionId) {
        String mappingKey = SESSION_REFRESH_PREFIX + sessionId;
        String activeRefreshTokenId = stringRedisTemplate.opsForValue().get(mappingKey);
        if (activeRefreshTokenId != null) {
            stringRedisTemplate.delete(REFRESH_PREFIX + activeRefreshTokenId);
        }
        stringRedisTemplate.delete(mappingKey);
        log.debug("[RedisTokenStore] 세션 리프레시 토큰 폐기 - sessionId: {}", sessionId);
    }
}

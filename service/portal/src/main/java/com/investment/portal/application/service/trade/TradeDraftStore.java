package com.investment.portal.application.service.trade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 확정 대기 중인 초안 보관소 (Redis).
 *
 * <p>초안에는 소유자와 대상 포트폴리오가 들어간다. 확정 단계는 클라이언트가 보낸
 * portfolioId 를 쓰지 않고 <b>여기 저장된 값</b>을 쓴다 — 그래야 남의 포트폴리오로
 * 기록을 밀어 넣을 수 없다.
 *
 * <p>테이블을 따로 만들지 않은 이유: 확정되지 않은 초안은 보존 가치가 없고,
 * TTL로 자동 소멸하는 편이 청소 코드를 안 만든다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeDraftStore {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "trade:draft:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public record Draft(String userId, Long portfolioId) {}

    public void save(String draftId, String userId, Long portfolioId) {
        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("userId", userId, "portfolioId", portfolioId));
            redis.opsForValue().set(KEY_PREFIX + draftId, json, TTL);
        } catch (Exception e) {
            throw new IllegalStateException("초안 저장에 실패했습니다", e);
        }
    }

    public Optional<Draft> find(String draftId) {
        if (draftId == null || draftId.isBlank()) {
            return Optional.empty();
        }
        String json = redis.opsForValue().get(KEY_PREFIX + draftId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object userId = map.get("userId");
            Object portfolioId = map.get("portfolioId");
            if (userId == null || portfolioId == null) {
                return Optional.empty();
            }
            return Optional.of(new Draft(
                    String.valueOf(userId),
                    ((Number) portfolioId).longValue()));
        } catch (Exception e) {
            log.warn("[TradeCapture] 초안 역직렬화 실패 - draftId: {}", draftId, e);
            return Optional.empty();
        }
    }

    /** 확정 완료된 초안은 즉시 제거해 같은 초안의 중복 저장을 막는다. */
    public void remove(String draftId) {
        redis.delete(KEY_PREFIX + draftId);
    }
}

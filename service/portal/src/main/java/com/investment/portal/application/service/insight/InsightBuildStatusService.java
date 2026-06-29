package com.investment.portal.application.service.insight;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class InsightBuildStatusService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(300);
    private static final Duration DONE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;

    public InsightBuildStatusService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String userId) {
        return "insight:build:" + userId;
    }

    /** PROCESSING 락 획득 시 true. 이미 진행 중이면 false. */
    public boolean tryAcquire(String userId) {
        Boolean ok = redis.opsForValue().setIfAbsent(key(userId), "PROCESSING", LOCK_TTL);
        return Boolean.TRUE.equals(ok);
    }

    public void markDone(String userId) {
        redis.opsForValue().set(key(userId), "DONE", DONE_TTL);
    }

    public void markFailed(String userId) {
        redis.opsForValue().set(key(userId), "FAILED", DONE_TTL);
    }

    public String status(String userId) {
        String v = redis.opsForValue().get(key(userId));
        return v == null ? "IDLE" : v;
    }
}

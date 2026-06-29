package com.investment.portal.insight;

import com.investment.portal.application.service.insight.InsightBuildStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InsightBuildStatusServiceTest {

    @Test
    void tryAcquireReturnsTrueWhenLockSet() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(eq("insight:build:u1"), eq("PROCESSING"), any(Duration.class))).thenReturn(true);

        assertThat(new InsightBuildStatusService(redis).tryAcquire("u1")).isTrue();
    }

    @Test
    void tryAcquireReturnsFalseWhenAlreadyLocked() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThat(new InsightBuildStatusService(redis).tryAcquire("u1")).isFalse();
    }

    @Test
    void statusReturnsIdleWhenKeyMissing() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("insight:build:u1")).thenReturn(null);

        assertThat(new InsightBuildStatusService(redis).status("u1")).isEqualTo("IDLE");
    }
}

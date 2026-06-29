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
        when(ops.setIfAbsent(eq("insight:build:u1"), eq("PROCESSING"), eq(Duration.ofSeconds(300)))).thenReturn(true);

        InsightBuildStatusService service = new InsightBuildStatusService(redis);
        assertThat(service.tryAcquire("u1")).isTrue();

        verify(ops).setIfAbsent(eq("insight:build:u1"), eq("PROCESSING"), eq(Duration.ofSeconds(300)));
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

    @Test
    void markDoneSetsDoneWith60sTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        new InsightBuildStatusService(redis).markDone("u1");

        verify(ops).set(eq("insight:build:u1"), eq("DONE"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void markFailedSetsFailedWith60sTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        new InsightBuildStatusService(redis).markFailed("u1");

        verify(ops).set(eq("insight:build:u1"), eq("FAILED"), eq(Duration.ofSeconds(60)));
    }
}

package com.investment.portal.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 기본 포트폴리오 자동 생성 경합 방지 락.
 *
 * <p>포트폴리오가 없을 때 조회 요청이 그 자리에서 하나를 만들어 준다. 그런데 종목 탭과
 * AI 어시스턴트가 거의 동시에 목록을 읽는 경우가 있어, 잠그지 않으면 "내 포트폴리오"가
 * 두 개 생긴다. InsightBuildStatusService 와 같은 방식이다.
 *
 * <p>TTL이 짧은 이유: 락을 놓친 요청은 잠시 빈 목록을 볼 뿐이고, 다음 조회에서 정상화된다.
 * 오래 잠가둘 이유가 없다.
 */
@Component
@RequiredArgsConstructor
public class DefaultPortfolioGuard {

    private static final Duration TTL = Duration.ofSeconds(10);
    private static final String KEY_PREFIX = "portfolio:default:";

    private final StringRedisTemplate redis;

    /** 생성 권한을 얻으면 true. 이미 다른 요청이 만드는 중이면 false. */
    public boolean tryAcquire(String userId) {
        Boolean ok = redis.opsForValue().setIfAbsent(KEY_PREFIX + userId, "1", TTL);
        return Boolean.TRUE.equals(ok);
    }

    public void release(String userId) {
        redis.delete(KEY_PREFIX + userId);
    }
}

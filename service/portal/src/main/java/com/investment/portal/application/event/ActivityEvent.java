package com.investment.portal.application.event;

/**
 * 감사/활동 로그 이벤트.
 * 비즈니스 코드에서 ApplicationEventPublisher 로 발행하면,
 * ActivityLogEventListener 가 커밋 이후(또는 트랜잭션 밖이면 즉시) 수신하여 DB에 적재한다.
 */
public record ActivityEvent(
        String userId,
        String actionType,
        String targetType,
        String targetId,
        String detail,
        String ip,
        String userAgent
) {
    /** 대상/컨텍스트 없이 액션만 기록 */
    public static ActivityEvent of(String userId, String actionType) {
        return new ActivityEvent(userId, actionType, null, null, null, null, null);
    }

    /** 대상 + 요약만 기록 */
    public static ActivityEvent of(String userId, String actionType, String targetType, String targetId, String detail) {
        return new ActivityEvent(userId, actionType, targetType, targetId, detail, null, null);
    }
}

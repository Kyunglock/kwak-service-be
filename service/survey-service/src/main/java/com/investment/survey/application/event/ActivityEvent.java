package com.investment.survey.application.event;

/**
 * 감사/활동 로그 이벤트. 발행 시 ActivityLogEventListener 가 공유 tbl_activity_log 에 적재.
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
    public static ActivityEvent of(String userId, String actionType, String targetType, String targetId, String detail) {
        return new ActivityEvent(userId, actionType, targetType, targetId, detail, null, null);
    }
}

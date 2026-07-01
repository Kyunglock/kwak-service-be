package com.investment.portal.application.dto.log;

import com.investment.portal.domain.entity.log.ActivityLog;

import java.time.LocalDateTime;

public record ActivityLogResponse(
        Long logId,
        String userId,
        String actionType,
        String targetType,
        String targetId,
        String detail,
        String ip,
        LocalDateTime regDt
) {
    public static ActivityLogResponse from(ActivityLog l) {
        return new ActivityLogResponse(
                l.getLogId(), l.getUserId(), l.getActionType(),
                l.getTargetType(), l.getTargetId(), l.getDetail(),
                l.getIp(), l.getRegDt());
    }
}

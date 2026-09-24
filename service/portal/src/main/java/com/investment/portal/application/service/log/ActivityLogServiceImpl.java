package com.investment.portal.application.service.log;

import com.investment.portal.application.dto.log.ActivityLogPage;
import com.investment.portal.application.dto.log.ActivityLogResponse;
import kwak.common.application.event.ActivityEvent;
import com.investment.portal.domain.entity.log.ActivityLog;
import com.investment.portal.domain.repository.log.ActivityLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ActivityLogMapper activityLogMapper;

    /**
     * reg_dt 를 DB의 NOW() 가 아니라 여기서 직접 계산해서 넘긴다. NOW()는 MySQL
     * 서버(세션)의 시간대를 타는데, JDBC URL의 serverTimezone 파라미터는 드라이버가
     * 값을 해석하는 방식만 바꿀 뿐 서버가 NOW()를 계산하는 시간대는 바꾸지 않는다 —
     * 서버 시간대가 KST로 맞춰져 있지 않으면 기록 시각이 밀린다.
     */
    @Override
    public void record(ActivityEvent e) {
        ActivityLog log = ActivityLog.builder()
                .userId(e.userId())
                .actionType(e.actionType())
                .targetType(e.targetType())
                .targetId(e.targetId())
                .detail(truncate(e.detail(), 1000))
                .ip(truncate(e.ip(), 45))
                .userAgent(truncate(e.userAgent(), 255))
                .regDt(LocalDateTime.now(KST))
                .build();
        activityLogMapper.insert(log);
    }

    @Override
    public ActivityLogPage getMyLogs(String userId, int page, int size) {
        int p = Math.max(0, page);
        int s = size <= 0 ? 20 : Math.min(size, 100);
        long total = activityLogMapper.countByUser(userId);
        List<ActivityLogResponse> content = activityLogMapper.findByUser(userId, p * s, s)
                .stream().map(ActivityLogResponse::from).toList();
        return toPage(content, p, s, total);
    }

    @Override
    public ActivityLogPage search(String requestingUserId, String targetUserId, String actionType, int page, int size) {
        int p = Math.max(0, page);
        int s = size <= 0 ? 20 : Math.min(size, 100);
        String excludeUserId = selfExclusion(requestingUserId, targetUserId);
        long total = activityLogMapper.countSearch(targetUserId, actionType, excludeUserId);
        List<ActivityLogResponse> content = activityLogMapper.search(targetUserId, actionType, excludeUserId, p * s, s)
                .stream().map(ActivityLogResponse::from).toList();
        return toPage(content, p, s, total);
    }

    /** targetUserId 로 요청자 본인을 명시적으로 지목한 게 아니면 요청자를 제외 대상으로 삼는다. */
    private String selfExclusion(String requestingUserId, String targetUserId) {
        boolean explicitlySelf = targetUserId != null && targetUserId.equals(requestingUserId);
        return explicitlySelf ? null : requestingUserId;
    }

    private ActivityLogPage toPage(List<ActivityLogResponse> content, int page, int size, long total) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new ActivityLogPage(content, page, size, total, totalPages);
    }

    private String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}

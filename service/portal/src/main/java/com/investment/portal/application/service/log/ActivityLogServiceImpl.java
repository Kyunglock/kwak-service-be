package com.investment.portal.application.service.log;

import com.investment.portal.application.dto.log.ActivityLogPage;
import com.investment.portal.application.dto.log.ActivityLogResponse;
import com.investment.portal.application.event.ActivityEvent;
import com.investment.portal.domain.entity.log.ActivityLog;
import com.investment.portal.domain.repository.log.ActivityLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogMapper activityLogMapper;

    @Override
    public void record(ActivityEvent e) {
        ActivityLog log = ActivityLog.builder()
                .userId(e.userId())
                .actionType(e.actionType())
                .targetType(e.targetType())
                .targetId(e.targetId())
                .detail(e.detail())
                .ip(truncate(e.ip(), 45))
                .userAgent(truncate(e.userAgent(), 255))
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
    public ActivityLogPage search(String userId, String actionType, int page, int size) {
        int p = Math.max(0, page);
        int s = size <= 0 ? 20 : Math.min(size, 100);
        long total = activityLogMapper.countSearch(userId, actionType);
        List<ActivityLogResponse> content = activityLogMapper.search(userId, actionType, p * s, s)
                .stream().map(ActivityLogResponse::from).toList();
        return toPage(content, p, s, total);
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

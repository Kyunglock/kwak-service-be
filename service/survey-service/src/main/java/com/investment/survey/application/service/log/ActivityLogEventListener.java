package com.investment.survey.application.service.log;

import com.investment.survey.application.event.ActivityEvent;
import com.investment.survey.domain.entity.log.ActivityLog;
import com.investment.survey.domain.repository.log.ActivityLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ActivityEvent 수신 → 공유 tbl_activity_log 적재.
 * 트랜잭션 커밋 이후에만 기록(롤백 시 유령 로그 방지), 실패는 삼켜 본 기능을 안 깨뜨림.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final ActivityLogMapper activityLogMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onActivityEvent(ActivityEvent e) {
        try {
            activityLogMapper.insert(ActivityLog.builder()
                    .userId(e.userId())
                    .actionType(e.actionType())
                    .targetType(e.targetType())
                    .targetId(e.targetId())
                    .detail(e.detail())
                    .ip(e.ip())
                    .userAgent(e.userAgent())
                    .build());
        } catch (Exception ex) {
            log.warn("[ActivityLog] 기록 실패 - action: {}, err: {}", e.actionType(), ex.getMessage());
        }
    }
}

package com.investment.portal.application.service.log;

import com.investment.portal.application.event.ActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ActivityEvent 수신 → 로그 적재.
 *
 * - 트랜잭션 안에서 발행되면 커밋 이후(AFTER_COMMIT)에만 기록 → 롤백 시 유령 로그 방지
 * - 트랜잭션 밖(예: 로그인)에서 발행되면 fallbackExecution=true 로 즉시 기록
 * - 로그 적재 실패가 본 기능을 깨뜨리지 않도록 예외를 삼킨다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final ActivityLogService activityLogService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onActivityEvent(ActivityEvent event) {
        try {
            activityLogService.record(event);
        } catch (Exception e) {
            log.warn("[ActivityLog] 기록 실패 - action: {}, err: {}", event.actionType(), e.getMessage());
        }
    }
}

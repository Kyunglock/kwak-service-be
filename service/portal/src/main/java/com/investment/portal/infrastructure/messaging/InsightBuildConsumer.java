package com.investment.portal.infrastructure.messaging;

import com.investment.portal.application.service.insight.InsightBuildStatusService;
import com.investment.portal.application.service.insight.InsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightBuildConsumer {

    private final InsightService insightService;
    private final InsightBuildStatusService statusService;

    @KafkaListener(topics = InsightBuildProducer.TOPIC, groupId = "insight-builder", concurrency = "1")
    public void onMessage(String userId) {
        log.info("[Insight] 빌드 소비 시작 - userId: {}", userId);
        try {
            insightService.executeBuild(userId);
            statusService.markDone(userId);
            log.info("[Insight] 빌드 완료 - userId: {}", userId);
        } catch (Exception e) {
            statusService.markFailed(userId);
            log.error("[Insight] 빌드 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }
}

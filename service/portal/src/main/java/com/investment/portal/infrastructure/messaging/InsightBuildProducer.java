package com.investment.portal.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightBuildProducer {

    public static final String TOPIC = "insight.build.requested";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String userId) {
        kafkaTemplate.send(TOPIC, userId, userId);
        log.info("[Insight] 빌드 요청 발행 - userId: {}", userId);
    }
}

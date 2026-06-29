package com.investment.portal.insight;

import com.investment.portal.infrastructure.messaging.InsightBuildProducer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.*;

class InsightBuildProducerTest {

    @Test
    void publishSendsUserIdAsKeyAndValue() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        new InsightBuildProducer(kafka).publish("u1");
        verify(kafka).send(InsightBuildProducer.TOPIC, "u1", "u1");
    }
}

package com.investment.portal.config;

import com.investment.portal.infrastructure.messaging.InsightBuildProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /** 단일 GPU 직렬화 보장을 위해 빌드 토픽을 1파티션으로 고정. */
    @Bean
    public NewTopic insightBuildTopic() {
        return TopicBuilder.name(InsightBuildProducer.TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}

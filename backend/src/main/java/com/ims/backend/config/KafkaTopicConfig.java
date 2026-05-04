package com.ims.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic auto-creation configuration.
 * Topics are created with replication for resilience if the cluster supports it.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${ims.kafka.topics.signals}")
    private String signalsTopic;

    @Value("${ims.kafka.topics.incidents}")
    private String incidentsTopic;

    @Bean
    public NewTopic signalsTopic() {
        return TopicBuilder.name(signalsTopic)
                .partitions(12)         // Parallelism headroom for 10k/sec burst
                .replicas(1)            // Set to 3 in production clusters
                .compact()
                .build();
    }

    @Bean
    public NewTopic incidentsTopic() {
        return TopicBuilder.name(incidentsTopic)
                .partitions(4)
                .replicas(1)
                .compact()
                .build();
    }
}

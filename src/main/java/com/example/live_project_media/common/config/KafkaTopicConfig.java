package com.example.live_project_media.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaTopicConfig {
    // VIDEO- Topic Configurations
    @Value("${app.kafka.topic.video-complete.name}")
    private String VIDEO_COMPLETE_TOPIC_NAME;
    @Value("${app.kafka.topic.video-complete.partitions}")
    private Integer VIDEO_COMPLETE_PARTITIONS;
    @Value("${app.kafka.topic.video-complete.replicas}")
    private Integer VIDEO_COMPLETE_REPLICAS;
    @Value("${app.kafka.topic.video-complete.retention-ms}")
    private String RETENTION_MS;
    @Value("${app.kafka.topic.video-complete.cleanup-policy}")
    private String CLEANUP_POLICY;

    @Bean
    public NewTopic videoCompleteTopic(){
        return TopicBuilder.name(VIDEO_COMPLETE_TOPIC_NAME)
                .partitions(VIDEO_COMPLETE_PARTITIONS)
                .replicas(VIDEO_COMPLETE_REPLICAS)
                .config("retention.ms", RETENTION_MS)
                .config("cleanup.policy", CLEANUP_POLICY)
                .build();
    }
}

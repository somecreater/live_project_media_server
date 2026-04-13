package com.example.live_project_media.common.config;

import com.example.live_project_media.dto.videoEncodeEvent;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@EnableKafka
@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, videoEncodeEvent> producerFactory(
            KafkaProperties properties
    ) {
        return new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties()
        );
    }

    @Bean
    public KafkaTemplate<String, videoEncodeEvent> kafkaTemplate(
            ProducerFactory<String, videoEncodeEvent> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}

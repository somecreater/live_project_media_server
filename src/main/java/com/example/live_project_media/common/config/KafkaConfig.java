package com.example.live_project_media.common.config;

import com.example.live_project_media.dto.VideoEncodingEvent;
import com.example.live_project_media.dto.VideoValidationEvent;
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

    //Video Validation Producer Configuration
    @Bean
    public ProducerFactory<String, VideoValidationEvent> producerFactory(
            KafkaProperties properties
    ) {
        return new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties()
        );
    }

    @Bean
    public KafkaTemplate<String, VideoValidationEvent> kafkaTemplate(
            ProducerFactory<String, VideoValidationEvent> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    //Video Encoding Producer Configuration
    @Bean
    public ProducerFactory<String, VideoEncodingEvent> EncodingProducerFactory(
            KafkaProperties properties
    ){
        return new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties()
        );
    }

    @Bean
    public KafkaTemplate<String, VideoEncodingEvent> EncodingKafkaTemplate(
            ProducerFactory<String, VideoEncodingEvent> producerFactory
    ){
        return new KafkaTemplate<>(producerFactory);
    }
}

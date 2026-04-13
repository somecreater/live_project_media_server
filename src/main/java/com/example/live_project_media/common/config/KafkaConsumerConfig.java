package com.example.live_project_media.common.config;

import com.example.live_project_media.dto.videoEncodeEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${app.kafka.consumer.partitions}")
    private Integer CONSUMER_PARTITIONS;

    @Bean
    public ConsumerFactory<String, videoEncodeEvent> consumerFactory(
            KafkaProperties properties){
        return new DefaultKafkaConsumerFactory<>(
                properties.buildConsumerProperties()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, videoEncodeEvent>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, videoEncodeEvent> consumerFactory
    ){
        ConcurrentKafkaListenerContainerFactory<String, videoEncodeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(CONSUMER_PARTITIONS); // 파티션 수 이하
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;

    }
}

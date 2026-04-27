package com.example.live_project_media.common.config;

import com.example.live_project_media.dto.VideoValidationEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${app.kafka.consumer.partitions}")
    private Integer CONSUMER_PARTITIONS;

    @Bean
    public ConsumerFactory<String, VideoValidationEvent> consumerFactory(
            KafkaProperties properties){
        Map<String, Object> props = new HashMap<>(properties.buildConsumerProperties());
        props.remove("spring.json.trusted.packages");
        props.remove("spring.json.use.type.headers");
        props.remove("spring.json.value.default.type");
        props.remove("spring.json.key.default.type");

        JacksonJsonDeserializer<VideoValidationEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(VideoValidationEvent.class);

        valueDeserializer.setUseTypeHeaders(false);
        valueDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VideoValidationEvent>
    videoValidateCompleteKafkaListenerContainerFactory(
            ConsumerFactory<String, VideoValidationEvent> consumerFactory
    ){
        ConcurrentKafkaListenerContainerFactory<String, VideoValidationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(CONSUMER_PARTITIONS); // 파티션 수 이하
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;

    }
}

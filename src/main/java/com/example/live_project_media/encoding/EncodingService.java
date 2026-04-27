package com.example.live_project_media.encoding;

import com.example.live_project_media.dto.VideoValidationEvent;
import com.example.live_project_media.encoding.Interface.EncodingServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EncodingService implements EncodingServiceInterface {

  @KafkaListener(
          topics = "video-complete-topic",
          groupId = "video-complete_group",
          containerFactory = "videoValidateCompleteKafkaListenerContainerFactory"
  )
  @Override
  public void consumerValidateMessage(VideoValidationEvent encodeEvent, Acknowledgment ack){
      try {
          log.info("Kafka Video Validate Complete Message Received by Kafka - Video Id: {}, Object Key: {}",
            encodeEvent.getVideo_id(),
            encodeEvent.getObject_key());
          ack.acknowledge();;
      } catch (Exception e) {
          log.error("Video consume failed", e);
      }
  }
}

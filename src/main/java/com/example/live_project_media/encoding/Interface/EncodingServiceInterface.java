package com.example.live_project_media.encoding.Interface;

import com.example.live_project_media.dto.VideoValidationEvent;
import org.springframework.kafka.support.Acknowledgment;

/**동영상 인코딩 기능(2026-04-27)*/
public interface EncodingServiceInterface {
  /**Kafka로부터 영상 업로드 및 검증 완료 메시지 수신*/
  public void consumerValidateMessage(VideoValidationEvent encodeEvent, Acknowledgment ack);
}

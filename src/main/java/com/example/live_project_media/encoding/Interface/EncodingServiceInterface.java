package com.example.live_project_media.encoding.Interface;

import com.example.live_project_media.dto.ParsedObjectKey;
import com.example.live_project_media.dto.VideoValidationEvent;
import org.springframework.kafka.support.Acknowledgment;

import java.io.IOException;
import java.util.List;

/**동영상 인코딩 기능(2026-04-27)*/
public interface EncodingServiceInterface {
  /**Kafka로부터 영상 업로드 및 검증 완료 메시지 수신*/
  public void consumerValidateMessage(VideoValidationEvent encodeEvent, Acknowledgment ack);
  /**동영상 인코딩 수행*/
  public void encodeVideo(Long videoId, String channelName, String objectKey);
  /**Object Key 해석*/
  public ParsedObjectKey parsedObjectKey(String objectKey);
  /**동영상 용량 파악 및 검증*/

  /**임시 작업 공간 확인*/

  /**R2 원본 파일 다운로드*/

  /**ffprobe로 저장된 동영상 파일 검증*/

  /**FFmpeg HLS 인코딩*/

  /**HLS 결과물 전체를 R2에 업로드*/

  /**인코딩 결과물 저장경로 구성*/

  /**동영상 파일 형식 파악*/

  /**원본 파일 삭제*/

  /**실패시 임시 결과물 삭제*/

  /**미디어 처리를 위한 명령어 실행*/
  public int runProcess(List<String> command) throws IOException, InterruptedException;
}

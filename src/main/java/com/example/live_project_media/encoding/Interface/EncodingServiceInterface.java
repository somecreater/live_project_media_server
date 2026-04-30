package com.example.live_project_media.encoding.Interface;

import com.example.live_project_media.dto.ParsedObjectKey;
import com.example.live_project_media.dto.VideoEncodingEvent;
import com.example.live_project_media.dto.VideoValidationEvent;
import org.springframework.kafka.support.Acknowledgment;

import java.io.IOException;
import java.nio.file.Path;
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
  public long getObjectSizeOrValidate(String objectKey);
  /**임시 작업 공간 확인*/
  public void validateFreeDiskSpace(Path tempRoot, long originalFileSize) throws IOException;
  /**R2 원본 파일 다운로드*/
  public void downloadOriginalVideo(String objectKey, Path inputFile);
  /**ffprobe로 저장된 동영상 파일 검증*/
  public void validateWithFfprobe(Path inputFile) throws IOException, InterruptedException;
  /**FFmpeg HLS 인코딩*/
  public void encodeToHls(Path inputFile, Path outputDir) throws IOException, InterruptedException;
  /**HLS 결과물 전체를 R2에 업로드*/
  public void uploadHlsDirectoryToR2(Path outputDir, String r2Prefix) throws IOException;
  /**인코딩 결과물 저장경로 구성*/
  public String buildTranscodedPrefix(String channelName, Long videoId);
  /**동영상 파일 형식 파악*/
  public String detectExtension(String objectKey);
  /**인코딩된 동영상 파일 형식 파악*/
  public String getContentType(Path path);
  /**R2에 있는 원볼 파일 삭제*/
  public void deleteOriginalVideo(String objectKey);
  /**임시 경로의 원본 파일 삭제*/
  public void deleteDirectoryQuietly(Path directory);
  /**미디어 처리를 위한 명령어 실행*/
  public int runProcess(List<String> command) throws IOException, InterruptedException;
  /**Kafka로 동영상 인코딩 완료 메시지 전송*/
  public void publishVideoEncodingCompleted(VideoEncodingEvent videoEncodingEvent);
}

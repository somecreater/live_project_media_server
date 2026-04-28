package com.example.live_project_media.encoding;

import com.example.live_project_media.dto.VideoValidationEvent;
import com.example.live_project_media.encoding.Interface.EncodingServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class EncodingService implements EncodingServiceInterface {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  @Value("${app.r2.video-bucket-name}")
  private String bucket_name;
  @Value("${app.r2.original-video-bucket-name}")
  private String original_video_folder;
  @Value("${app.r2.transcoding-video-bucket-name}")
  private String transcoding_video_folder;
  @Value("${app.file.video_limit_size}")
  private Long video_limit_size;

  /**aws ebs 미 구성에 따른 임시 파일 경로*/
  private final String TMP_FILE_DIRECTORY="";

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


  public int runProcess(List<String> command) throws IOException, InterruptedException{
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(process.getInputStream())
      )) {
          String line;
          while ((line = reader.readLine()) != null) {
              log.info("[ffmpeg] {}", line);
          }
      }

      return process.waitFor();
  }
}

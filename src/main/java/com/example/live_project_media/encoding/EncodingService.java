package com.example.live_project_media.encoding;

import com.example.live_project_media.dto.ParsedObjectKey;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private final String TMP_FILE_DIRECTORY="C:\\live_project\\temp";

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

          ParsedObjectKey parsedObjectKey = parsedObjectKey(encodeEvent.getObject_key());

          encodeVideo(
                  parsedObjectKey.getVideoId(),
                  parsedObjectKey.getChannelName(),
                  encodeEvent.getObject_key()
          );

          ack.acknowledge();
      } catch (Exception e) {
          log.error("Video consume failed", e);
      }
  }

  @Override
  public void encodeVideo(Long videoId, String channelName, String objectKey){
      Path workDir = null;

      try {
          Path tempRoot = Paths.get(TMP_FILE_DIRECTORY);
          Files.createDirectories(tempRoot);

          workDir = Files.createTempDirectory(
                  tempRoot,
                  "encoding-" + channelName + "-" + videoId + "-"
          );

      }catch (IOException e){
          log.error("Video encoding failed. videoId={}, channelName={}, objectKey={}",
                  videoId,
                  channelName,
                  objectKey,
                  e);
      }

  }

  @Override
  public ParsedObjectKey parsedObjectKey(String objectKey){
      if (objectKey == null || objectKey.isBlank()) {
          throw new IllegalArgumentException("objectKey is blank");
      }
      String[] parts = objectKey.split("/", 3);

      if (parts.length != 3) {
          throw new IllegalArgumentException("Invalid objectKey format: " + objectKey);
      }

      String prefix = parts[0];
      String channelName = parts[1];
      String filePart = parts[2];

      int underscoreIndex = filePart.indexOf("_");

      if (underscoreIndex <= 0) {
          throw new IllegalArgumentException("Cannot parse videoId and title from objectKey: " + objectKey);
      }

      Long videoId = Long.parseLong(filePart.substring(0, underscoreIndex));
      String title = filePart.substring(underscoreIndex + 1);

      return new ParsedObjectKey(prefix + "/", channelName, videoId, title);
  }

  @Override
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

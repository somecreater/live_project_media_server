package com.example.live_project_media.encoding;

import com.example.live_project_media.dto.ParsedObjectKey;
import com.example.live_project_media.dto.VideoEncodingEvent;
import com.example.live_project_media.dto.VideoValidationEvent;
import com.example.live_project_media.encoding.Interface.EncodingServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class EncodingService implements EncodingServiceInterface {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  @Value("${app.kafka.topic.video-encoding.name}")
  private String VIDEO_ENCODING_TOPIC_NAME;
  private final ApplicationEventPublisher publisher;
  private final KafkaTemplate<String, VideoEncodingEvent> kafkaTemplate;

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

  private static final long MB = 1024L * 1024L;
  private static final int DISK_SPACE_MULTIPLIER = 3;

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
          ParsedObjectKey parsedObjectKey = parsedObjectKey(objectKey);

          Path tempRoot = Paths.get(TMP_FILE_DIRECTORY);
          Files.createDirectories(tempRoot);

          workDir = Files.createTempDirectory(
                  tempRoot,
                  "encoding-" + channelName + "-" + videoId + "-"
          );

          Path inputFile = workDir.resolve("input" + detectExtension(objectKey));
          Path outputDir = workDir.resolve("hls");

          Files.createDirectories(outputDir);

          log.info("Encoding work directory created. workDir={}", workDir);

          long originalFileSize = getObjectSizeOrValidate(objectKey);

          validateFreeDiskSpace(tempRoot, originalFileSize);

          downloadOriginalVideo(objectKey, inputFile);

          validateWithFfprobe(inputFile);

          encodeToHls(inputFile, outputDir);

          String outputPrefix = buildTranscodedPrefix(
                  parsedObjectKey.getChannelName(),
                  parsedObjectKey.getVideoId()
          );

          uploadHlsDirectoryToR2(outputDir, outputPrefix);

          deleteOriginalVideo(objectKey);

          publisher.publishEvent(new VideoEncodingEvent(videoId, channelName, objectKey, outputPrefix));
          log.info("Video encoding completed. videoId={}, hlsPath={}",
                  videoId,
                  outputPrefix + "master.m3u8");

      } catch (Exception e) {
          /**
           * TODO DB 상태 FAILED 업데이트
           *
           * video.status = FAILED
           * video.errorMessage = e.getMessage()
           */

          log.error("Video encoding failed. videoId={}, channelName={}, objectKey={}",
                  videoId,
                  channelName,
                  objectKey,
                  e);

          throw new RuntimeException("Video encoding failed. videoId=" + videoId, e);

      } finally {
          if (workDir != null) {
              deleteDirectoryQuietly(workDir);
          }
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
  public long getObjectSizeOrValidate(String objectKey){
      HeadObjectRequest request = HeadObjectRequest.builder()
              .bucket(bucket_name)
              .key(objectKey)
              .build();

      HeadObjectResponse response = s3Client.headObject(request);

      if (response.contentLength() == null || response.contentLength() <= 0) {
          throw new IllegalStateException("Invalid R2 object size. key=" + objectKey);
      }

      if (video_limit_size != null && video_limit_size > 0 && response.contentLength() > video_limit_size) {
          throw new IllegalStateException(
                  "Video file size exceeds limit. fileSize=" + response.contentLength() +
                          ", limit=" + video_limit_size
          );
      }

      log.info("Original object size. key={}, size={}MB",
              objectKey,
              response.contentLength() / MB);

      return response.contentLength();
  }

  @Override
  public void validateFreeDiskSpace(Path tempRoot, long originalFileSize) throws IOException {
      FileStore fileStore = Files.getFileStore(tempRoot);

      long usableSpace = fileStore.getUsableSpace();
      long requiredSpace = originalFileSize * DISK_SPACE_MULTIPLIER;

      log.info("Disk space check. usable={}MB, required={}MB",
              usableSpace / MB,
              requiredSpace / MB);

      if (usableSpace < requiredSpace) {
          throw new IllegalStateException(
                  "Not enough disk space for encoding. usable=" + usableSpace +
                          ", required=" + requiredSpace
          );
      }
  }

  @Override
  public void downloadOriginalVideo(String objectKey, Path inputFile){
      GetObjectRequest request = GetObjectRequest.builder()
              .bucket(bucket_name)
              .key(objectKey)
              .build();

      log.info("Original video download start. key={}, target={}", objectKey, inputFile);

      s3Client.getObject(request, ResponseTransformer.toFile(inputFile));

      log.info("Original video download completed. target={}", inputFile);
  }

  @Override
  public void validateWithFfprobe(Path inputFile) throws IOException, InterruptedException{
      List<String> command = List.of(
              "ffprobe",
              "-v", "error",
              "-select_streams", "v:0",
              "-show_entries", "stream=codec_name,width,height,duration",
              "-of", "default=noprint_wrappers=1",
              inputFile.toAbsolutePath().toString()
      );

      int exitCode = runProcess(command);

      if (exitCode != 0) {
          throw new IllegalStateException("ffprobe validation failed. exitCode=" + exitCode);
      }

      log.info("ffprobe validation success. input={}", inputFile);
  }

  @Override
  public void encodeToHls(Path inputFile, Path outputDir) throws IOException, InterruptedException{
      Path masterPlaylist = outputDir.resolve("master.m3u8");
      Path segmentPattern = outputDir.resolve("segment_%05d.ts");

      List<String> command = List.of(
              "ffmpeg",
              "-y",

              "-i", inputFile.toAbsolutePath().toString(),

              "-c:v", "libx264",
              "-preset", "veryfast",
              "-crf", "23",

              /**
               * 1080p 초과 영상은 1080p로 제한
               * 1080p 이하 영상은 원본 비율 유지
               */
              "-vf", "scale='min(1920,iw)':-2",

              "-c:a", "aac",
              "-b:a", "128k",
              "-ac", "2",

              "-f", "hls",
              "-hls_time", "6",
              "-hls_playlist_type", "vod",
              "-hls_segment_filename", segmentPattern.toAbsolutePath().toString(),

              masterPlaylist.toAbsolutePath().toString()
      );

      log.info("FFmpeg HLS encoding start. input={}, output={}",
              inputFile,
              masterPlaylist);

      int exitCode = runProcess(command);

      if (exitCode != 0) {
          throw new IllegalStateException("ffmpeg HLS encoding failed. exitCode=" + exitCode);
      }

      log.info("FFmpeg HLS encoding success. output={}", masterPlaylist);
  }

  public void uploadHlsDirectoryToR2(Path outputDir, String r2Prefix) throws IOException{
      try (Stream<Path> paths = Files.walk(outputDir)) {
          paths.filter(Files::isRegularFile)
                  .forEach(path -> {
                      try {
                          String relativePath = outputDir.relativize(path)
                                  .toString()
                                  .replace("\\", "/");

                          String key = r2Prefix + relativePath;
                          String contentType = getContentType(path);

                          PutObjectRequest request = PutObjectRequest.builder()
                                  .bucket(bucket_name)
                                  .key(key)
                                  .contentType(contentType)
                                  .build();

                          s3Client.putObject(request, RequestBody.fromFile(path));

                          log.info("HLS file uploaded to R2. key={}, contentType={}",
                                  key,
                                  contentType);

                      } catch (Exception e) {
                          throw new RuntimeException("Failed to upload HLS file to R2. path=" + path, e);
                      }
                  });
      }
  }

  @Override
  public String buildTranscodedPrefix(String channelName, Long videoId){
      String normalizedFolder = transcoding_video_folder;

      if (!normalizedFolder.endsWith("/")) {
          normalizedFolder += "/";
      }

      return normalizedFolder + channelName + "/" + videoId + "/";
  }

  @Override
  public String detectExtension(String objectKey){
      String lower = objectKey.toLowerCase();

      if (lower.endsWith(".mp4")) {
          return ".mp4";
      }

      if (lower.endsWith(".mov")) {
          return ".mov";
      }

      /**
       * 현재 object key에 확장자가 없다면 기본 mp4로 저장.
       * FFmpeg는 보통 컨테이너 내부 정보를 보고 처리할 수 있음.
       */
      return ".mp4";
  }

  @Override
  public String getContentType(Path path){
      String fileName = path.getFileName().toString().toLowerCase();

      if (fileName.endsWith(".m3u8")) {
          return "application/vnd.apple.mpegurl";
      }

      if (fileName.endsWith(".ts")) {
          return "video/mp2t";
      }

      if (fileName.endsWith(".mp4")) {
          return "video/mp4";
      }

      if (fileName.endsWith(".mov")) {
          return "video/quicktime";
      }

      return "application/octet-stream";
  }

  @Override
  public void deleteOriginalVideo(String objectKey) {
      DeleteObjectRequest request = DeleteObjectRequest.builder()
              .bucket(bucket_name)
              .key(objectKey)
              .build();

      s3Client.deleteObject(request);

      log.info("Original video deleted from R2. key={}", objectKey);
  }

  @Override
  public void deleteDirectoryQuietly(Path directory){
      try (Stream<Path> paths = Files.walk(directory)) {
          paths.sorted(Comparator.reverseOrder())
                  .forEach(path -> {
                      try {
                          Files.deleteIfExists(path);
                      } catch (IOException e) {
                          log.warn("Failed to delete temp file. path={}", path, e);
                      }
                  });

          log.info("Temp directory deleted. path={}", directory);

      } catch (IOException e) {
          log.warn("Failed to delete temp directory. path={}", directory, e);
      }
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

  @EventListener
  @Override
  public void publishVideoEncodingCompleted(VideoEncodingEvent videoEncodingEvent){
      kafkaTemplate.send(
        VIDEO_ENCODING_TOPIC_NAME,
        videoEncodingEvent.getObjectKey(),
        videoEncodingEvent
      );

      log.info("Kafka Video Validation Message Produced to Kafka - Object KEY: {}, Video ID: {}",
              videoEncodingEvent.getObjectKey(),
              videoEncodingEvent.getVideoId());
  }
}

package com.dev.videoStreaming.video.application.conversion;

import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import com.dev.videoStreaming.video.domain.videoReadyEvent;
import com.dev.videoStreaming.video.domain.videoStatus;
import com.dev.videoStreaming.video.infrastructure.ffmpeg;
import io.minio.MinioClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class videoconversionservice {

    private final ffmpeg ffmpeg;
    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;

    public videoconversionservice(ffmpeg ffmpeg, MinioClient minioClient, RabbitTemplate rabbitTemplate) {
        this.ffmpeg = ffmpeg;
        this.minioClient = minioClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "video-upload")
    public void processVideo(videoReadyEvent event) {
        String objectName = event.getObjectName();
        log.info("Received video for conversion: {}", objectName);
        
        String tempDir = System.getProperty("java.io.tmpdir");
        File conversionDir = new File(tempDir, UUID.randomUUID().toString());
        conversionDir.mkdirs();

        File InputFile = new File(conversionDir, objectName);
        try{
            log.info("Downloading original video {} from MinIO to {}", objectName, InputFile.getAbsolutePath());
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket("videos")
                    .object(objectName)
                    .build());
            Files.copy(stream, InputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Download complete.");
        }catch (Exception e){
            log.error("Failed to download video {}", objectName, e);
            throw new RuntimeException("Failed to download video", e);
        }

        String outputFileName = InputFile.getName() + ".m3u8";
        File OutputFile = new File(conversionDir, outputFileName);

        log.info("Starting FFmpeg conversion for {}", InputFile.getAbsolutePath());
        ffmpeg.convertVideo(InputFile.getAbsolutePath(), OutputFile.getAbsolutePath());
        log.info("FFmpeg conversion completed. Output playlist: {}", OutputFile.getAbsolutePath());

        // Upload all generated files (.m3u8 and .ts)
        File[] files = conversionDir.listFiles();
        if (files != null) {
            log.info("Uploading {} generated chunks to MinIO...", files.length);
            for (File file : files) {
                if (file.getName().endsWith(".m3u8") || file.getName().endsWith(".ts")) {
                    String contentType = file.getName().endsWith(".m3u8") ? "application/vnd.apple.mpegurl" : "video/MP2T";
                    try {
                        log.debug("Uploading chunk: {}", file.getName());
                        minioClient.putObject(
                            PutObjectArgs.builder()
                                .bucket("videos")
                                .object(file.getName())
                                .stream(Files.newInputStream(file.toPath()), file.length(), -1)
                                .contentType(contentType)
                                .build()
                        );
                    } catch (Exception e) {
                        log.error("Failed to upload video chunk: {}", file.getName(), e);
                        throw new RuntimeException("Failed to upload video chunk: " + file.getName(), e);
                    }
                }
            }
            log.info("All chunks uploaded successfully.");
        }

        // Clean up temp files
        log.info("Cleaning up temporary directory: {}", conversionDir.getAbsolutePath());
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        conversionDir.delete();

        // Remove original video from MinIO
        try{
            log.info("Removing original video {} from MinIO...", objectName);
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket("videos")
                    .object(objectName)
                    .build()
            );
            log.info("Original video removed successfully.");
        }catch (Exception e){
            log.error("Failed to delete original video {}", objectName, e);
            throw new RuntimeException("Failed to delete original video", e);
        }

        videoReadyEvent CompletedVideo = new videoReadyEvent(event.getTitle(), event.getDescription(), event.getThumbnailObjectName(), outputFileName, videoStatus.READY);
        log.info("Video processing complete. Sending video-ready event for {}", outputFileName);
        rabbitTemplate.convertAndSend("video-exchange", "video-ready", CompletedVideo);
    }
}

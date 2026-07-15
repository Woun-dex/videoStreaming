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
        System.out.println("Received video for conversion: " + objectName);
        
        String tempDir = System.getProperty("java.io.tmpdir");
        File conversionDir = new File(tempDir, UUID.randomUUID().toString());
        conversionDir.mkdirs();

        File InputFile = new File(conversionDir, objectName);
        try{
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket("videos")
                    .object(objectName)
                    .build());
            Files.copy(stream, InputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch (Exception e){
            throw new RuntimeException("Failed to download video", e);
        }

        String outputFileName = InputFile.getName() + ".m3u8";
        File OutputFile = new File(conversionDir, outputFileName);

        ffmpeg.convertVideo(InputFile.getAbsolutePath(), OutputFile.getAbsolutePath());

        // Upload all generated files (.m3u8 and .ts)
        File[] files = conversionDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".m3u8") || file.getName().endsWith(".ts")) {
                    String contentType = file.getName().endsWith(".m3u8") ? "application/vnd.apple.mpegurl" : "video/MP2T";
                    try {
                        minioClient.putObject(
                            PutObjectArgs.builder()
                                .bucket("videos")
                                .object(file.getName())
                                .stream(Files.newInputStream(file.toPath()), file.length(), -1)
                                .contentType(contentType)
                                .build()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to upload video chunk: " + file.getName(), e);
                    }
                }
            }
        }

        // Clean up temp files
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        conversionDir.delete();

        // Remove original video from MinIO
        try{
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket("videos")
                    .object(objectName)
                    .build()
            );
        }catch (Exception e){
            throw new RuntimeException("Failed to delete original video", e);
        }

        videoReadyEvent CompletedVideo = new videoReadyEvent(outputFileName, videoStatus.READY);
        rabbitTemplate.convertAndSend("video-exchange", "video-ready", CompletedVideo);
    }
}

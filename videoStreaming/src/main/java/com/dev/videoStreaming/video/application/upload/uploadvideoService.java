package com.dev.videoStreaming.video.application.upload;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.dev.videoStreaming.video.domain.videoMetadata;
import com.dev.videoStreaming.video.domain.videoReadyEvent;
import com.dev.videoStreaming.video.domain.videoStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import com.dev.videoStreaming.video.port.videoRepository;

@Slf4j
@Service
public class uploadvideoService {

    public uploadvideoService(MinioClient minioClient, RabbitTemplate rabbitTemplate, videoRepository videoRepository) {
        this.minioClient = minioClient;
        this.rabbitTemplate = rabbitTemplate;
        this.videoRepository = videoRepository;
    }

    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;
    private final videoRepository videoRepository;
    private final String bucketName = "videos";
    private final String exchangeName = "video-exchange";
    private final String routingKey = "video-upload";

    public void uploadVideo(MultipartFile file) {
        String objectName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video to MinIO", e);
        }

        videoReadyEvent message = new videoReadyEvent(objectName, videoStatus.PENDING);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }

    @RabbitListener(queues = "video-ready")
    public void videoReady(videoReadyEvent event) {
        log.info("Video ready: " + event);
        videoMetadata metadata = videoMetadata.builder()
            .videoId(UUID.randomUUID().toString())
            .title(event.getObjectName())
            .description("")
            .thumbnailUrl("")
            .objectName(event.getObjectName())
            .views(0L)
            .likes(0L)
            .build();
        
        videoRepository.save(metadata);
    }

}

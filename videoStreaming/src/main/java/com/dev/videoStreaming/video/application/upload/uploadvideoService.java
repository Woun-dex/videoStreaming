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
import com.dev.videoStreaming.video.port.VideoRepository;

@Slf4j
@Service
public class UploadvideoService {

    public UploadvideoService(MinioClient minioClient, RabbitTemplate rabbitTemplate, VideoRepository videoRepository) {
        this.minioClient = minioClient;
        this.rabbitTemplate = rabbitTemplate;
        this.videoRepository = videoRepository;
    }

    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;
    private final VideoRepository videoRepository;
    private final String bucketName = "videos";
    private final String bucketName2 = "thumbnails";
    private final String baseUrl = "http://localhost:9000";
    private final String exchangeName = "video-exchange";
    private final String routingKey = "video-upload";

    public String uploadVideo(MultipartFile file , String title , String description , MultipartFile thumbnail ) {
        String objectName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        String thumbnailObjectName = UUID.randomUUID().toString() + "-" + thumbnail.getOriginalFilename();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

            minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName2)
                        .object(thumbnailObjectName)
                        .stream(thumbnail.getInputStream(), thumbnail.getSize(), -1)
                        .contentType(thumbnail.getContentType())
                        .build());

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video to MinIO", e);
        }

        videoReadyEvent message = new videoReadyEvent(title, description, thumbnailObjectName ,objectName, videoStatus.PENDING);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);

        return objectName;
    }

    @RabbitListener(queues = "video-ready")
    public void videoReady(videoReadyEvent event) {
        log.info("Video ready: " + event);
        videoMetadata metadata = videoMetadata.builder()
            .videoId(UUID.randomUUID().toString())
            .title(event.getTitle())
            .description(event.getDescription())
            .thumbnailUrl(baseUrl + "/" + bucketName2 + "/" + event.getThumbnailObjectName())
            .objectName(event.getObjectName())
            .views(0L)
            .likes(0L)
            .build();
        
        videoRepository.save(metadata);
    }

}

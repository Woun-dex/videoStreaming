package com.dev.videoStreaming.video.application.upload;

import com.dev.videoStreaming.video.domain.videoMetadata;
import com.dev.videoStreaming.video.domain.videoReadyEvent;
import com.dev.videoStreaming.video.domain.videoStatus;
import com.dev.videoStreaming.video.port.VideoRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class uploadvideoServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private UploadvideoService service;

    @Test
    void uploadVideo_Success_UploadsToMinioAndSendsMessage() throws Exception {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test-video.mp4");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("video/mp4");
        
        InputStream stream = new ByteArrayInputStream("dummy data".getBytes());
        when(file.getInputStream()).thenReturn(stream);

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.getOriginalFilename()).thenReturn("thumb.jpg");
        when(thumbnail.getSize()).thenReturn(512L);
        when(thumbnail.getContentType()).thenReturn("image/jpeg");
        when(thumbnail.getInputStream()).thenReturn(new ByteArrayInputStream("thumb data".getBytes()));

        // Act
        service.uploadVideo(file, "test title", "test description", thumbnail);

        // Assert
        verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
        
        ArgumentCaptor<videoReadyEvent> eventCaptor = ArgumentCaptor.forClass(videoReadyEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("video-exchange"), 
            eq("video-upload"), 
            eventCaptor.capture()
        );

        videoReadyEvent sentEvent = eventCaptor.getValue();
        assertNotNull(sentEvent);
        assertEquals(videoStatus.PENDING, sentEvent.getStatus());
        assertTrue(sentEvent.getObjectName().endsWith("test-video.mp4"));
    }

    @Test
    void uploadVideo_MinioFailure_ThrowsRuntimeException() throws Exception {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test-video.mp4");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("video/mp4");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

        doThrow(new RuntimeException("Minio error")).when(minioClient).putObject(any(PutObjectArgs.class));

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.getOriginalFilename()).thenReturn("thumb.jpg");
        lenient().when(thumbnail.getSize()).thenReturn(512L);
        lenient().when(thumbnail.getContentType()).thenReturn("image/jpeg");
        lenient().when(thumbnail.getInputStream()).thenReturn(new ByteArrayInputStream("thumb data".getBytes()));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.uploadVideo(file, "test title", "test description", thumbnail);
        });

        assertTrue(exception.getMessage().contains("Failed to upload video to MinIO"));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void videoReady_ValidEvent_SavesMetadataToDatabase() {
        // Arrange
        videoReadyEvent event = new videoReadyEvent("title", "description", "thumbnail.jpg", "generated-id-test-video.mp4", videoStatus.READY);

        // Act
        service.videoReady(event);

        // Assert
        ArgumentCaptor<videoMetadata> metadataCaptor = ArgumentCaptor.forClass(videoMetadata.class);
        verify(videoRepository, times(1)).save(metadataCaptor.capture());

        videoMetadata savedMetadata = metadataCaptor.getValue();
        assertNotNull(savedMetadata);
        assertNotNull(savedMetadata.getVideoId());
        assertEquals("title", savedMetadata.getTitle());
        assertEquals("generated-id-test-video.mp4", savedMetadata.getObjectName());
        assertEquals(0L, savedMetadata.getViews());
        assertEquals(0L, savedMetadata.getLikes());
    }
}

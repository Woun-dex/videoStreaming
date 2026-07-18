package com.dev.videoStreaming.video.application.conversion;

import com.dev.videoStreaming.video.domain.videoReadyEvent;
import com.dev.videoStreaming.video.domain.videoStatus;
import com.dev.videoStreaming.video.infrastructure.ffmpeg;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.minio.GetObjectResponse;

@ExtendWith(MockitoExtension.class)
public class videoconversionserviceTest {

    @Mock
    private ffmpeg ffmpegService;

    @Mock
    private MinioClient minioClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private videoconversionservice service;

    @Test
    void processVideo_Success_ConvertsAndUploads() throws Exception {
        // Arrange
        String objectName = "test-video.mp4";
        videoReadyEvent event = new videoReadyEvent("title", "description", "thumbnail.jpg", objectName, videoStatus.PENDING);

        // Mock minio download
        GetObjectResponse dummyResponse = mock(GetObjectResponse.class);
        lenient().when(dummyResponse.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        lenient().when(dummyResponse.read(any(byte[].class))).thenReturn(-1);
        lenient().when(dummyResponse.read()).thenReturn(-1);
        
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(dummyResponse);

        // Mock ffmpeg to simulate creating .m3u8 and .ts files
        doAnswer(invocation -> {
            String input = invocation.getArgument(0);
            String output = invocation.getArgument(1);
            
            // Create dummy output files in the conversion directory
            java.io.File m3u8File = new java.io.File(output);
            m3u8File.createNewFile();
            
            java.io.File tsFile = new java.io.File(m3u8File.getParentFile(), "segment0.ts");
            tsFile.createNewFile();
            
            return null;
        }).when(ffmpegService).convertVideo(anyString(), anyString());

        // Act
        service.processVideo(event);

        // Assert
        // Verify ffmpeg was called
        verify(ffmpegService, times(1)).convertVideo(anyString(), anyString());
        
        // Verify 2 files were uploaded to minio (.m3u8 and .ts)
        verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
        
        // Verify original file was deleted from minio
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        
        // Verify message was sent to rabbitmq
        ArgumentCaptor<videoReadyEvent> eventCaptor = ArgumentCaptor.forClass(videoReadyEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
            eq("video-exchange"), 
            eq("video-ready"), 
            eventCaptor.capture()
        );

        videoReadyEvent sentEvent = eventCaptor.getValue();
        assertNotNull(sentEvent);
        assertEquals(videoStatus.READY, sentEvent.getStatus());
        assertEquals("test-video.mp4.m3u8", sentEvent.getObjectName());
    }

    @Test
    void processVideo_DownloadFailure_ThrowsException() throws Exception {
        // Arrange
        videoReadyEvent event = new videoReadyEvent("title", "description", "thumbnail.jpg", "test.mp4", videoStatus.PENDING);
        doThrow(new RuntimeException("Minio Download Failed")).when(minioClient).getObject(any(GetObjectArgs.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.processVideo(event);
        });

        assertTrue(exception.getMessage().contains("Failed to download video"));
        verify(ffmpegService, never()).convertVideo(anyString(), anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void processVideo_FfmpegFailure_ThrowsException() throws Exception {
        // Arrange
        videoReadyEvent event = new videoReadyEvent("title", "description", "thumbnail.jpg", "test.mp4", videoStatus.PENDING);
        GetObjectResponse dummyResponse = mock(GetObjectResponse.class);
        lenient().when(dummyResponse.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        lenient().when(dummyResponse.read(any(byte[].class))).thenReturn(-1);
        lenient().when(dummyResponse.read()).thenReturn(-1);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(dummyResponse);

        doThrow(new RuntimeException("FFmpeg failed")).when(ffmpegService).convertVideo(anyString(), anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.processVideo(event);
        });

        assertEquals("FFmpeg failed", exception.getMessage());
        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}

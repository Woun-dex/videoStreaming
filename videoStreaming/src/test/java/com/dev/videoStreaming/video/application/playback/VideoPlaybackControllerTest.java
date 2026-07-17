package com.dev.videoStreaming.video.application.playback;

import com.dev.videoStreaming.video.domain.videoMetadata;
import com.dev.videoStreaming.video.port.videoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VideoPlaybackControllerTest {

    @Mock
    private videoRepository videoRepository;

    @InjectMocks
    private VideoPlaybackController videoPlaybackController;

    @Test
    void getVideoUrl_WhenVideoExists_ReturnsCorrectUrl() {
        // Arrange
        String videoId = "test-video-id";
        String objectName = "test-video.m3u8";
        videoMetadata metadata = new videoMetadata();
        metadata.setVideoId(videoId);
        metadata.setObjectName(objectName);
        
        when(videoRepository.findByVideoId(videoId)).thenReturn(Optional.of(metadata));

        // Act
        String url = videoPlaybackController.getVideoUrl(videoId);

        // Assert
        assertEquals("http://localhost:9000/videos/" + objectName, url);
    }

    @Test
    void getVideoUrl_WhenVideoDoesNotExist_ThrowsRuntimeException() {
        // Arrange
        String videoId = "non-existent-video-id";
        when(videoRepository.findByVideoId(videoId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            videoPlaybackController.getVideoUrl(videoId);
        });
        
        assertEquals("Video not found", exception.getMessage());
    }
}

package com.dev.videoStreaming.video.infrastructure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ffmpegTest {

    @Test
    void convertVideo_InvalidInput_ThrowsException() {
        ffmpeg ffmpegService = new ffmpeg();
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ffmpegService.convertVideo("non_existent_input.mp4", "output.m3u8");
        });
        
        // It will either fail to start (if ffmpeg is not installed) 
        // or fail to convert (if ffmpeg is installed but input is missing).
        assertTrue(
            exception.getMessage().contains("Failed to convert video") || 
            exception.getMessage().contains("FFmpeg conversion failed with exit code")
        );
    }
}

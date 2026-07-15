package com.dev.videoStreaming.video.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Slf4j
@Component
public class ffmpeg {

    private static final Logger logger = LoggerFactory.getLogger(ffmpeg.class);


    public void convertVideo(String inputPath, String outputPath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-i", inputPath, outputPath);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Video converted successfully");
            } else {
                logger.error("Video conversion failed with exit code: " + exitCode);
                throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert video", e);
        }
    }
}
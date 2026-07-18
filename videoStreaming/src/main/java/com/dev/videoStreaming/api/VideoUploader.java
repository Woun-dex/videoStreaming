package com.dev.videoStreaming.api;

import com.dev.videoStreaming.video.application.upload.UploadvideoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/video")
public class VideoUploader {

    private final UploadvideoService uploadVideoService;

    public VideoUploader(UploadvideoService uploadVideoService) {
        this.uploadVideoService = uploadVideoService;
    }

    @PostMapping("/upload")
    public String uploadVideo(@RequestParam("file") MultipartFile file, @RequestParam("thumbnail") MultipartFile thumbnail, @RequestParam("title") String title, @RequestParam("description") String description) throws IOException {
        if (file == null || file.isEmpty()) {
            return "Please select a video file to upload";
        }

        String videoId = uploadVideoService.uploadVideo(file, title, description, thumbnail);
        return "Video uploaded successfully! Video ID: " + videoId;
    }
}

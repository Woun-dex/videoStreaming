package com.dev.videoStreaming.video.application.playback;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dev.videoStreaming.video.domain.videoMetadata;
import com.dev.videoStreaming.video.port.VideoRepository;

@RestController
@RequestMapping("/api/videos")
public class VideoPlaybackController {

    private final VideoRepository videoRepository;

    public VideoPlaybackController(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @GetMapping("/{videoId}/url")
    public String getVideoUrl(@PathVariable String videoId) {
        videoMetadata metadata = videoRepository.findByVideoId(videoId)
            .orElseThrow(() -> new RuntimeException("Video not found"));
            
        // The objectName is your .m3u8 file
        String objectName = metadata.getObjectName(); 
        
        // Return the direct MinIO URL
        return "http://localhost:9000/videos/" + objectName;
    }
}

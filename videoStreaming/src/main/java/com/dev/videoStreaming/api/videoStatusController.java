package com.dev.videoStreaming.api;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.videoStreaming.video.application.playback.VideoStatsService;


@RestController
@RequestMapping("/api/video")
public class VideoStatusController {

    private final VideoStatsService videoStatusService;

    public VideoStatusController(VideoStatsService videoStatusService) {
        this.videoStatusService = videoStatusService;
    }

    @PostMapping("/{videoId}/Like/{userId}")
    public void Like(@PathVariable String videoId,@PathVariable String userId){
        videoStatusService.toggleLike(videoId, userId);
    }

   
    

    
}

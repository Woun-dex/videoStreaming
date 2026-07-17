package com.dev.videoStreaming.api;


import com.dev.videoStreaming.video.domain.videoMetadata;
import com.dev.videoStreaming.video.port.VideoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    public VideoController(VideoRepository videoRepository){
        this.videoRepository = videoRepository;
    }

    private final VideoRepository videoRepository ;

    @GetMapping
    public Page<videoMetadata> getVideos(Pageable pageable){
        return videoRepository.findAll(pageable);
    }

    @GetMapping("/search/{request}")
    public Page<videoMetadata> searchVideo(String request , Pageable pageable){
        return videoRepository.findByObjectNameContaining(request,pageable);
    }

}

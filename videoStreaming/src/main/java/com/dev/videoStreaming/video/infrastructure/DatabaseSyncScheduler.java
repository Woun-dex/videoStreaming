package com.dev.videoStreaming.video.infrastructure;

import org.springframework.stereotype.Component;

import com.dev.videoStreaming.video.port.VideoRepository;
import com.dev.videoStreaming.video.domain.videoMetadata;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.redisson.api.RedissonClient;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RSet;

@Component
@EnableScheduling
public class DatabaseSyncScheduler {

    private final RedissonClient redissonClient ;
    private final VideoRepository videoRepository;

    public DatabaseSyncScheduler(RedissonClient redissonClient, VideoRepository videoRepository) {
        this.redissonClient = redissonClient;
        this.videoRepository = videoRepository;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncStats() {
       
        List<videoMetadata> videos = videoRepository.findAll();

        for(videoMetadata video : videos) {
            String videoId = video.getVideoId();
            
            RAtomicLong viewCount = redissonClient.getAtomicLong("video:viewCount:" + videoId);
            RSet<String> likes = redissonClient.getSet("video:likes:" + videoId);

            long currentViews = viewCount.get();
            long currentLikes = likes.size();

            if (currentViews > 0 || currentLikes != video.getLikes()) {
                
                video.setViews(video.getViews() + currentViews);
                video.setLikes(currentLikes);
                videoRepository.save(video);
                viewCount.set(0); 
            }
        }
        
    }


}
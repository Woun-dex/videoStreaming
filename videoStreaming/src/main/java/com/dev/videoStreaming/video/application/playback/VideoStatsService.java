package com.dev.videoStreaming.video.application.playback;

import org.springframework.stereotype.Service;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

@Service
public class VideoStatsService {

    private final RedissonClient redissonClient;
    
    public VideoStatsService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void incrementView(String videoId){
        RAtomicLong views = redissonClient.getAtomicLong("video:views:" + videoId);
        views.incrementAndGet();
    }

     public boolean toggleLike(String videoId, String userId) {
        RSet<String> likers = redissonClient.getSet("video:likers:" + videoId);
        
        // If the user is already in the set, they are "unliking" the video
        if (likers.contains(userId)) {
            likers.remove(userId);
            return false; // unliked
        } else {
            likers.add(userId);
            return true; // liked
        }
    }



    

  
}

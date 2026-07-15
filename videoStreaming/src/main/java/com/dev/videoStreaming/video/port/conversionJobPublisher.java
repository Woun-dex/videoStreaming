package com.dev.videoStreaming.video.port;

import com.dev.videoStreaming.video.domain.videoReadyEvent;

public interface conversionJobPublisher {
    public void publish(videoReadyEvent event);
}

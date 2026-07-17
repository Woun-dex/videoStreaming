package com.dev.videoStreaming.video.infrastructure;

import com.dev.videoStreaming.video.domain.videoMetadata;
import com.dev.videoStreaming.video.port.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DatabaseSyncSchedulerTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private RAtomicLong viewCountMock;

    @Mock
    private RSet<String> likesMock;

    @InjectMocks
    private DatabaseSyncScheduler scheduler;

    @Test
    void syncStats_NoVideosInDatabase_DoesNothing() {
        // Arrange
        when(videoRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        scheduler.syncStats();

        // Assert
        verify(redissonClient, never()).getAtomicLong(anyString());
        verify(redissonClient, never()).getSet(anyString());
        verify(videoRepository, never()).save(any());
    }

    @Test
    void syncStats_WithViewsAndNoNewLikes_UpdatesViewsAndSaves() {
        // Arrange
        videoMetadata video = new videoMetadata();
        video.setVideoId("vid1");
        video.setViews(100L);
        video.setLikes(50L);

        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(redissonClient.getAtomicLong("video:viewCount:vid1")).thenReturn(viewCountMock);
        when(redissonClient.<String>getSet("video:likes:vid1")).thenReturn(likesMock);
        
        when(viewCountMock.get()).thenReturn(5L); // 5 new views
        when(likesMock.size()).thenReturn(50);    // same likes

        // Act
        scheduler.syncStats();

        // Assert
        verify(videoRepository, times(1)).save(video);
        verify(viewCountMock, times(1)).set(0); // View count is reset
        assertEquals(105L, video.getViews());
        assertEquals(50L, video.getLikes());
    }

    @Test
    void syncStats_WithNewLikesAndNoNewViews_UpdatesLikesAndSaves() {
        // Arrange
        videoMetadata video = new videoMetadata();
        video.setVideoId("vid2");
        video.setViews(200L);
        video.setLikes(10L);

        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(redissonClient.getAtomicLong("video:viewCount:vid2")).thenReturn(viewCountMock);
        when(redissonClient.<String>getSet("video:likes:vid2")).thenReturn(likesMock);
        
        when(viewCountMock.get()).thenReturn(0L); // no new views
        when(likesMock.size()).thenReturn(15);    // 5 new likes

        // Act
        scheduler.syncStats();

        // Assert
        verify(videoRepository, times(1)).save(video);
        verify(viewCountMock, times(1)).set(0); 
        assertEquals(200L, video.getViews());
        assertEquals(15L, video.getLikes());
    }

    @Test
    void syncStats_NoNewViewsOrLikes_DoesNotSave() {
        // Arrange
        videoMetadata video = new videoMetadata();
        video.setVideoId("vid3");
        video.setViews(300L);
        video.setLikes(20L);

        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(redissonClient.getAtomicLong("video:viewCount:vid3")).thenReturn(viewCountMock);
        when(redissonClient.<String>getSet("video:likes:vid3")).thenReturn(likesMock);
        
        when(viewCountMock.get()).thenReturn(0L);
        when(likesMock.size()).thenReturn(20);

        // Act
        scheduler.syncStats();

        // Assert
        verify(videoRepository, never()).save(any());
        verify(viewCountMock, never()).set(anyLong()); // View count should not be reset
    }

    @Test
    void syncStats_WithNewViewsAndNewLikes_UpdatesBothAndSaves() {
        // Arrange
        videoMetadata video = new videoMetadata();
        video.setVideoId("vid4");
        video.setViews(400L);
        video.setLikes(30L);

        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(redissonClient.getAtomicLong("video:viewCount:vid4")).thenReturn(viewCountMock);
        when(redissonClient.<String>getSet("video:likes:vid4")).thenReturn(likesMock);
        
        when(viewCountMock.get()).thenReturn(10L); // 10 new views
        when(likesMock.size()).thenReturn(35);    // 5 new likes

        // Act
        scheduler.syncStats();

        // Assert
        verify(videoRepository, times(1)).save(video);
        verify(viewCountMock, times(1)).set(0); // View count is reset
        assertEquals(410L, video.getViews());
        assertEquals(35L, video.getLikes());
    }
}

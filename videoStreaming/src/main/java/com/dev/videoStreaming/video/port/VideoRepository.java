package com.dev.videoStreaming.video.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dev.videoStreaming.video.domain.videoMetadata;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<videoMetadata, Long> {
    Optional<videoMetadata> findByObjectName(String objectName);
    Optional<videoMetadata> findByVideoId(String videoId);
    Page<videoMetadata> findByObjectNameContaining(String search , Pageable pageable);

}

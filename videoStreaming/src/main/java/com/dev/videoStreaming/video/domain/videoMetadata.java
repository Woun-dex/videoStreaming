package com.dev.videoStreaming.video.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "video_metadata")
@Getter
@Setter
public class videoMetadata {

    @Id
    private String videoId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private String size;
    private String views;
    private String likes;

}

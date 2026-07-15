package com.dev.videoStreaming.video.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class videoReadyEvent {

    private String objectName;
    private videoStatus status;

}

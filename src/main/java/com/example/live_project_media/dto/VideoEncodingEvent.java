package com.example.live_project_media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class VideoEncodingEvent {
    private Long videoId;
    private String channelName;
    private String objectKey;
    private String resultKey;

    public VideoEncodingEvent(
            @JsonProperty("videoId") Long videoId,
            @JsonProperty("channelName") String channelName,
            @JsonProperty("objectKey") String objectKey,
            @JsonProperty("resultKey") String resultKey){
        this.videoId = videoId;
        this.channelName = channelName;
        this.objectKey = objectKey;
        this.resultKey = resultKey;
    }
}

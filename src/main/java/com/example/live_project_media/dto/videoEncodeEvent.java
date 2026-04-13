package com.example.live_project_media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class videoEncodeEvent {
    private final Long video_id;
    private final String object_key;

    public videoEncodeEvent(
            @JsonProperty("video_id") Long video_id,
            @JsonProperty("object_key") String object_key
    ){
        this.video_id=video_id;
        this.object_key=object_key;
    }
}

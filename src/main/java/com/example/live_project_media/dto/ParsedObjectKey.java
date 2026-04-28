package com.example.live_project_media.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParsedObjectKey {
    String prefix;
    String channelName;
    Long videoId;
    String title;
}

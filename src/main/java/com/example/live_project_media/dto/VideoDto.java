package com.example.live_project_media.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoDto {
    private Long id;
    private String title;
    private String description;
    private String file_type;
    private Long size;
    private boolean allow_comments;
    private Visibility visibility;
    private ProcessingStatus processingStatus;
    private Status status;
    private int duration_seconds;
    private int like;
    private int unlike;
    private int view_count;
    private String originalObjectKey;
    private String hlsObjectKey;
    private Long expectedFileSize;
    private String thumbnail_url;
    private Long channel_id;
    private String channel_name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

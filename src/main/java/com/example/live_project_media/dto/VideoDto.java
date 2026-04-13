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
    private Status status;
    private int duration_seconds;
    private String hls_url;
    private String thumbnail_url;
    private String presigned_url;
    private String channel_name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.example.graduation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Integer isRead;
    private Long relatedId;
    private LocalDateTime createdAt;
}


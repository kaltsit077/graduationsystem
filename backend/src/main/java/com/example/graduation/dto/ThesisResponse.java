package com.example.graduation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThesisResponse {
    private Long id;
    private Long topicId;
    private String topicTitle;
    private Long studentId;
    private String studentName;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    /** 环节代码，见 CollabStage */
    private String stage;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


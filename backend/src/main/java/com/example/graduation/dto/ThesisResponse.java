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
    /** 下载是否仍在有效期内 */
    private Boolean downloadable;
    /** 下载有效期截止时间 */
    private LocalDateTime downloadExpireAt;
    /** 是否已过期 */
    private Boolean downloadExpired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


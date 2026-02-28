package com.example.graduation.dto;

import lombok.Data;

@Data
public class ThesisUploadRequest {
    private Long topicId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}


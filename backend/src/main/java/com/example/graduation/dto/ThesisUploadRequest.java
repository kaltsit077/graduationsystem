package com.example.graduation.dto;

import lombok.Data;

@Data
public class ThesisUploadRequest {
    /** 可选；为空时后端默认 {@link com.example.graduation.entity.CollabStage#PRE_DEFENSE_THESIS} */
    private String stage;
    private Long topicId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}


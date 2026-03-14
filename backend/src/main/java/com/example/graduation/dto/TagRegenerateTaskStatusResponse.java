package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class TagRegenerateTaskStatusResponse {
    /**
     * PENDING / RUNNING / SUCCEEDED / FAILED
     */
    private String status;

    /**
     * 失败原因（status=FAILED 时有值）
     */
    private String error;

    /**
     * 生成的标签（status=SUCCEEDED 时有值）
     */
    private List<UserTagResponse> tags;
}


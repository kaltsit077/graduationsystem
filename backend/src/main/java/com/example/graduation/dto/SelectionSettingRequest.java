package com.example.graduation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SelectionSettingRequest {

    /** 全局开关：true 表示允许选题，false 表示全部关闭 */
    private Boolean enabled;

    /** 选题开放开始时间（可选，NULL 表示不限制开始时间） */
    private LocalDateTime startTime;

    /** 选题开放结束时间（可选，NULL 表示不限制结束时间） */
    private LocalDateTime endTime;
}


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

    /** 毕业季总时间窗开始；导师设置的各环节时间不得早于此（可空表示不限制） */
    private LocalDateTime graduationSeasonStart;

    /** 毕业季总时间窗结束；导师设置的各环节时间不得晚于此（可空表示不限制） */
    private LocalDateTime graduationSeasonEnd;
}


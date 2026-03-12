package com.example.graduation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SelectionSettingResponse {

    private Boolean enabled;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 服务器当前判断的“此刻是否开放” */
    private Boolean openNow;
}


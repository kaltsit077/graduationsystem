package com.example.graduation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollabStageWindowItemRequest {
    /** {@link com.example.graduation.entity.CollabStage#name()} */
    private String stage;
    /** 未设置起止则表示该环节「未开放」，由导师后续配置 */
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
}

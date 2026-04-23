package com.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("collab_stage_window")
public class CollabStageWindow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    /** 与 {@link CollabStage#name()} 一致 */
    private String stage;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private LocalDateTime updatedAt;
}

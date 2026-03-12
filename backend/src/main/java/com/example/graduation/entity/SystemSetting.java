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
@TableName("system_setting")
public class SystemSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 选题系统是否启用全局开关（false 表示完全关闭） */
    private Boolean selectionEnabled;

    /** 选题开放开始时间（可选） */
    private LocalDateTime selectionStartTime;

    /** 选题开放结束时间（可选） */
    private LocalDateTime selectionEndTime;

    private LocalDateTime updatedAt;
}


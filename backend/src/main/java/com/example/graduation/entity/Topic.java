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
@TableName("topic")
public class Topic {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teacherId;
    
    private String title;
    
    private String description;
    
    private TopicStatus status;
    
    private Integer maxApplicants;
    
    private Integer currentApplicants;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum TopicStatus {
        DRAFT,           // 草稿
        PENDING_REVIEW,  // 待审核
        REJECTED,        // 已驳回
        OPEN,            // 已开放
        CLOSED           // 已关闭
    }
}


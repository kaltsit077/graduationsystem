package com.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("topic_application")
public class TopicApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long topicId;
    
    private Long studentId;
    
    private ApplicationStatus status;
    
    private String remark;
    
    private String teacherFeedback;
    
    private BigDecimal matchScore; // 匹配度得分（0-1之间）
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum ApplicationStatus {
        PENDING,   // 待审核
        APPROVED,  // 已通过
        REJECTED   // 已拒绝
    }
}


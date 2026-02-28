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
@TableName("topic_review")
public class TopicReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long topicId;
    
    private Long adminId;
    
    private ReviewResult result;
    
    private String comment;
    
    private LocalDateTime createdAt;
    
    public enum ReviewResult {
        PASS, REJECT
    }
}


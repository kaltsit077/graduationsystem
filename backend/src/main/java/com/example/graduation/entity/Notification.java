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
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String type;
    
    private String title;
    
    private String content;
    
    private Integer isRead; // 0-未读，1-已读
    
    private Long relatedId; // 关联对象ID（如选题ID、申请ID等）
    
    private LocalDateTime createdAt;
}


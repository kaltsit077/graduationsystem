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
@TableName("thesis")
public class Thesis {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long topicId;
    
    private Long studentId;
    
    private String fileUrl;
    
    private String fileName;
    
    private Long fileSize;
    
    private ThesisStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum ThesisStatus {
        UPLOADED,  // 已上传
        REVIEWED   // 已评审
    }
}


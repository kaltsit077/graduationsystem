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
@TableName("teacher_profile")
public class TeacherProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String researchDirection;
    
    private Integer maxStudentCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}


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
@TableName("student_profile")
public class StudentProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String major;

    /**
     * 主修课程/已修课程（用于增强“专业侧标签生成”信号）
     */
    private String majorCourses;
    
    private String grade;
    
    private String interestDesc;

    /**
     * 标签生成模式：MAJOR / INTEREST / BOTH
     */
    private String tagMode;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}


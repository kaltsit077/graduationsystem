package com.example.graduation.dto;

import com.example.graduation.entity.MentorApplication;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MentorApplicationResponse {

    private Long id;

    private Long studentId;

    private Long teacherId;

    /** 学生姓名（导师端展示用） */
    private String studentName;

    /** 导师姓名（学生端展示用） */
    private String teacherName;

    private MentorApplication.Status status;

    private String reason;

    private String teacherComment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}


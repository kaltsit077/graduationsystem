package com.example.graduation.dto;

import com.example.graduation.entity.MentorApplication;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MentorApplicationResponse {

    private Long id;

    private Long studentId;

    private Long teacherId;

    private MentorApplication.Status status;

    private String reason;

    private String teacherComment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}


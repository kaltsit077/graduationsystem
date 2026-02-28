package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ApplicationResponse {
    private Long id;
    private Long topicId;
    private String topicTitle;
    private Long studentId;
    private String studentName;
    private String status;
    private String remark;
    private String teacherFeedback;
    private BigDecimal matchScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TopicResponse {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private String title;
    private String description;
    private String status;
    private Integer maxApplicants;
    private Integer currentApplicants;
    /**
     * 学生端可选：当前登录学生与该选题的匹配度（0.30-1.00）。
     * 导师端/管理员端可能为空。
     */
    private BigDecimal matchScore;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


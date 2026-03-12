package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ApplicationResponse {
    private Long id;
    private Long topicId;
    private String topicTitle;
    /** 选题所属导师ID，便于前端直接确定消息收件人 */
    private Long teacherId;
    private Long studentId;
    private String studentName;
    private String status;
    private String remark;
    private String teacherFeedback;
    private BigDecimal matchScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


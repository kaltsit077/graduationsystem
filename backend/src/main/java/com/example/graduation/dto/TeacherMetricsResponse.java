package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeacherMetricsResponse {
    private Long teacherId;
    private String teacherName;
    private Integer totalStudents;
    private BigDecimal avgScore;
    private BigDecimal excellentRatio;
    private BigDecimal failRatio;
    /** 学生满意度均分（来自 thesis_evaluation.student_score，0-100） */
    private BigDecimal avgStudentScore;
}


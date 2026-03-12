package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopicMetricsResponse {
    private Long topicId;
    private String topicTitle;
    private BigDecimal avgScore;
    private Integer totalStudents;
    private BigDecimal excellentRatio;
    private BigDecimal failRatio;
}


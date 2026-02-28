package com.example.graduation.dto;

import lombok.Data;

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
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


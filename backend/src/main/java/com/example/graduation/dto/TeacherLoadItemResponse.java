package com.example.graduation.dto;

import lombok.Data;

@Data
public class TeacherLoadItemResponse {
    private Long teacherId;
    private String realName;
    private Integer currentStudents;
    private Integer maxStudents;
    private Integer openTopics;
    private Integer totalTopics;
}


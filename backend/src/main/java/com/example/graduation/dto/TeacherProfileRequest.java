package com.example.graduation.dto;

import lombok.Data;

@Data
public class TeacherProfileRequest {
    private String realName;
    private String title;
    private String researchDirection;
    private Integer maxStudentCount;
}


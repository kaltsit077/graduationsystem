package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class TeacherProfileResponse {
    private Long userId;
    private String username;
    private String realName;
    private String title;
    private String researchDirection;
    private Integer maxStudentCount;
    private List<UserTagResponse> tags;
}


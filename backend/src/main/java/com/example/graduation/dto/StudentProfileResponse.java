package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentProfileResponse {
    private Long userId;
    private String major;
    private String grade;
    private String interestDesc;
    private List<UserTagResponse> tags;
}


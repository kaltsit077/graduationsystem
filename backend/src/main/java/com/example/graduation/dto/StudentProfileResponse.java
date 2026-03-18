package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentProfileResponse {
    private Long userId;
    private String username;
    private String realName;
    private String major;
    private String majorCourses;
    private String grade;
    private String interestDesc;
    /**
     * 标签生成模式：MAJOR / INTEREST / BOTH
     */
    private String tagMode;
    private List<UserTagResponse> tags;
}


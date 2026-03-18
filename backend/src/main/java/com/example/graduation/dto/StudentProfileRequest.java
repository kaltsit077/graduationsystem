package com.example.graduation.dto;

import lombok.Data;

@Data
public class StudentProfileRequest {
    private String realName;
    private String major;
    private String majorCourses;
    private String grade;
    private String interestDesc;
    /**
     * 标签生成模式：MAJOR / INTEREST / BOTH
     */
    private String tagMode;
}


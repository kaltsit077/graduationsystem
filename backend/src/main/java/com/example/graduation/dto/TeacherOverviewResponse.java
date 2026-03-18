package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TeacherOverviewResponse {

    private Long teacherId;

    private String realName;

    private String title;

    private String researchDirection;

    private Integer maxStudentCount;

    private Integer currentStudentCount;

    private Integer openTopicCount;

    private List<String> tags;

    private Integer totalStudents;

    private BigDecimal avgScore;

    private BigDecimal excellentRatio;

    private BigDecimal failRatio;

    /**
     * 学生视角下的导师匹配度（0-1），仅在学生端查询时计算。
     */
    private BigDecimal matchScore;
}


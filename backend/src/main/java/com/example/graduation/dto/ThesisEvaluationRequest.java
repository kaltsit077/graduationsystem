package com.example.graduation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ThesisEvaluationRequest {
    @NotNull(message = "论文ID不能为空")
    private Long thesisId;

    @Min(value = 0, message = "成绩不能小于0")
    @Max(value = 100, message = "成绩不能大于100")
    private Integer score;

    private Integer defenseScore;

    private Integer reviewScore;

    private String gradeLevel;

    private String comment;
}


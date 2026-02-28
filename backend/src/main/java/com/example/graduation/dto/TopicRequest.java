package com.example.graduation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TopicRequest {
    @NotBlank(message = "选题标题不能为空")
    private String title;
    
    private String description;
    
    @NotNull(message = "最大申请人数不能为空")
    private Integer maxApplicants;
    
    private List<String> tags;
}


package com.example.graduation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TopicDuplicateCheckRequest {
    private Long topicId; // 编辑时传入，新建时为空
    @NotBlank(message = "选题标题不能为空")
    private String title;
    private String description;
}


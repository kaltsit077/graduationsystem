package com.example.graduation.dto;

import lombok.Data;

@Data
public class TopicDuplicateCheckResponse {
    private boolean passed;
    private double maxSimilarity;
    private Long similarTopicId;
    private String similarTopicTitle;
}


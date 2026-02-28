package com.example.graduation.dto;

import lombok.Data;

@Data
public class TopicReviewRequest {
    private String result; // PASS or REJECT
    private String comment;
}


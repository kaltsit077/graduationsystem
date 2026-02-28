package com.example.graduation.dto;

import lombok.Data;

@Data
public class StatsResponse {
    private Long applicationCount;
    private Long unreadNotificationCount;
    private Long approvedApplicationCount;
    private Long topicCount;
    private Long pendingApplicationCount;
    private Long pendingReviewTopicCount;
}


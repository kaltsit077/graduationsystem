package com.example.graduation.dto;

import com.example.graduation.entity.ChangeRequest;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeRequestResponse {

    private Long id;

    private Long studentId;

    private Long currentApplicationId;

    private ChangeRequest.ChangeType type;

    private String reason;

    private Long targetTopicId;

    private Long targetTeacherId;

    private ChangeRequest.ChangeStatus status;

    private ChangeRequest.Decision teacherDecision;

    private String teacherComment;

    private ChangeRequest.Decision adminDecision;

    private String adminComment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}


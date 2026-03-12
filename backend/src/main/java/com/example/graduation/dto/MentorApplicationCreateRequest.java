package com.example.graduation.dto;

import lombok.Data;

@Data
public class MentorApplicationCreateRequest {

    private Long teacherId;

    private String reason;
}


package com.example.graduation.dto;

import lombok.Data;

@Data
public class ApplicationProcessRequest {
    private String status; // APPROVED or REJECTED
    private String feedback;
}


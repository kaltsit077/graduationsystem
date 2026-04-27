package com.example.graduation.dto;

import lombok.Data;

@Data
public class ApplicationProcessRequest {
    private String status; // APPROVED or REJECTED（普通申请与结题审核共用）
    private String feedback;
}


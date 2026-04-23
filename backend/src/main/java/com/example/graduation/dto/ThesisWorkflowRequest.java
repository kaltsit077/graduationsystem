package com.example.graduation.dto;

import lombok.Data;

@Data
public class ThesisWorkflowRequest {
    /** APPROVE | NEED_REVISION */
    private String decision;
}

package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserTagResponse {
    private String tagName;
    private BigDecimal weight;
}


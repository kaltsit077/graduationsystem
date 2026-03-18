package com.example.graduation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserTagResponse {
    private String tagName;
    /**
     * 标签类型：MAJOR / INTEREST
     */
    private String tagType;
    private BigDecimal weight;
}


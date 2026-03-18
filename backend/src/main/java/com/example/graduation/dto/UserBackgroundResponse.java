package com.example.graduation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBackgroundResponse {
    private String backgroundUrl;
    private Integer backgroundScale;
    private Integer backgroundPosX;
    private Integer backgroundPosY;
    private Double bgOverlayAlpha;
    private Double contentAlpha;
    private Integer contentBlur;
}


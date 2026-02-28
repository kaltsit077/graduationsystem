package com.example.graduation.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminResetPasswordRequest {
    @NotEmpty(message = "请至少选择一个账号")
    private List<Long> userIds;
}

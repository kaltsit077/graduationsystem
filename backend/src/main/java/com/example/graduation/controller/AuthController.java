package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.LoginRequest;
import com.example.graduation.dto.LoginResponse;
import com.example.graduation.dto.RegisterRequest;
import com.example.graduation.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }
    
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ApiResponse.success(response);
    }
}


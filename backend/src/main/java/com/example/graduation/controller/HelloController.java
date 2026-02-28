package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("backend-ok");
    }
    
    /**
     * 验证 token 并获取当前用户信息
     * 需要认证，用于验证 token 有效性
     */
    @GetMapping("/auth/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        UserContext.setUserInfo(request);
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();
        String role = UserContext.getRole();
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", username);
        userInfo.put("role", role);
        
        return ApiResponse.success(userInfo);
    }
}



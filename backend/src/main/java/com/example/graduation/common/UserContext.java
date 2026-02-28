package com.example.graduation.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具类，用于获取当前登录用户信息
 */
public class UserContext {
    
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> roleHolder = new ThreadLocal<>();
    
    /**
     * 从request中获取用户信息（由JWT过滤器设置）
     */
    public static void setUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        String role = (String) request.getAttribute("role");
        
        if (userId != null) {
            userIdHolder.set(userId);
        }
        if (username != null) {
            usernameHolder.set(username);
        }
        if (role != null) {
            roleHolder.set(role);
        }
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return userIdHolder.get();
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return usernameHolder.get();
    }
    
    /**
     * 获取当前用户角色
     */
    public static String getRole() {
        return roleHolder.get();
    }
    
    /**
     * 清除ThreadLocal，防止内存泄漏
     */
    public static void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
        roleHolder.remove();
    }
}


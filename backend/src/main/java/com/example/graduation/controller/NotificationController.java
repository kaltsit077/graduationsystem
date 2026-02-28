package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.NotificationResponse;
import com.example.graduation.entity.Notification;
import com.example.graduation.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 获取用户通知列表
     */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<Notification> notifications = notificationService.getUserNotifications(userId, isRead, limit);
        
        List<NotificationResponse> responses = notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        long count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(count);
    }
    
    /**
     * 标记通知为已读
     */
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.success(null);
    }
    
    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(null);
    }
    
    /**
     * 转换Notification实体为NotificationResponse
     */
    private NotificationResponse convertToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setIsRead(notification.getIsRead());
        response.setRelatedId(notification.getRelatedId());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}


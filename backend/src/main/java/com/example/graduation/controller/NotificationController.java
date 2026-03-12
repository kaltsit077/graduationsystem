package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.NotificationResponse;
import com.example.graduation.entity.Notification;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.UserMapper;
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
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TopicApplicationMapper topicApplicationMapper;
    
    @Autowired
    private TopicMapper topicMapper;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 获取当前用户角色（STUDENT / TEACHER / ADMIN）
     */
    private String getCurrentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : null;
    }
    
    /**
     * 获取用户通知列表
     */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long relatedId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<Notification> notifications = notificationService.getUserNotifications(userId, isRead, type, relatedId, limit);
        
        List<NotificationResponse> responses = notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * 发送聊天消息（学生 <-> 导师）
     */
    @PostMapping("/chat")
    public ApiResponse<Void> sendChat(
            @RequestParam(required = false) Long targetUserId,
            @RequestParam String content,
            @RequestParam(required = false) Long relatedId,
            HttpServletRequest request) {
        Long fromUserId = getCurrentUserId(request);
        User fromUser = userMapper.selectById(fromUserId);
        String senderName = fromUser != null ? fromUser.getRealName() : "系统";
        
        String title = "来自 " + senderName + " 的消息";
        String type = "CHAT";
        
        Long realTargetUserId = targetUserId;
        
        // 如未显式指定 targetUserId，则根据当前用户角色和 relatedId 自动推断接收人
        if (realTargetUserId == null || realTargetUserId <= 0L) {
            if (relatedId == null) {
                return ApiResponse.error("缺少消息接收人信息：targetUserId 和 relatedId 不能同时为空");
            }
            TopicApplication application = topicApplicationMapper.selectById(relatedId);
            if (application == null) {
                return ApiResponse.error("选题申请不存在，无法推断消息接收人");
            }
            String role = getCurrentUserRole(request);
            if ("STUDENT".equalsIgnoreCase(role)) {
                Topic topic = topicMapper.selectById(application.getTopicId());
                if (topic == null || topic.getTeacherId() == null) {
                    return ApiResponse.error("未找到对应导师信息，无法发送消息");
                }
                realTargetUserId = topic.getTeacherId();
            } else if ("TEACHER".equalsIgnoreCase(role)) {
                if (application.getStudentId() == null) {
                    return ApiResponse.error("未找到对应学生信息，无法发送消息");
                }
                realTargetUserId = application.getStudentId();
            } else {
                return ApiResponse.error("当前角色不支持发送聊天消息");
            }
        }
        
        notificationService.createNotification(realTargetUserId, type, title, content, relatedId);
        return ApiResponse.success(null);
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


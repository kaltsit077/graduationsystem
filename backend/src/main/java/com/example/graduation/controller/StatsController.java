package com.example.graduation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.StatsResponse;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    
    @Autowired
    private TopicApplicationMapper applicationMapper;
    
    @Autowired
    private TopicMapper topicMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 获取学生端统计数据
     */
    @GetMapping("/student")
    public ApiResponse<StatsResponse> getStudentStats(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        StatsResponse stats = new StatsResponse();
        
        // 我的申请数量
        long applicationCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStudentId, userId)
        );
        stats.setApplicationCount(applicationCount);
        
        // 未读通知数量
        long unreadCount = notificationService.getUnreadCount(userId);
        stats.setUnreadNotificationCount(unreadCount);
        
        // 已通过申请数量
        long approvedCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStudentId, userId)
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
        );
        stats.setApprovedApplicationCount(approvedCount);
        
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取导师端统计数据
     */
    @GetMapping("/teacher")
    public ApiResponse<StatsResponse> getTeacherStats(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        StatsResponse stats = new StatsResponse();
        
        // 我的选题数量
        long topicCount = topicMapper.selectCount(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getTeacherId, userId)
        );
        stats.setTopicCount(topicCount);
        
        // 待处理申请数量（需要查询所有选题的待处理申请）
        // 这里简化处理，实际应该关联查询
        long pendingCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.PENDING)
        );
        stats.setPendingApplicationCount(pendingCount);
        
        // 已通过申请数量
        long approvedCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
        );
        stats.setApprovedApplicationCount(approvedCount);
        
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取管理员端统计数据
     */
    @GetMapping("/admin")
    public ApiResponse<StatsResponse> getAdminStats() {
        StatsResponse stats = new StatsResponse();
        
        // 待审核选题数量
        long pendingReviewCount = topicMapper.selectCount(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getStatus, Topic.TopicStatus.PENDING_REVIEW)
        );
        stats.setPendingReviewTopicCount(pendingReviewCount);
        
        return ApiResponse.success(stats);
    }
}


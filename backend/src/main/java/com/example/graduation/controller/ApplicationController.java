package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.ApplicationProcessRequest;
import com.example.graduation.dto.ApplicationRequest;
import com.example.graduation.dto.ApplicationResponse;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.ApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    
    @Autowired
    private ApplicationService applicationService;
    
    @Autowired
    private TopicMapper topicMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 学生提交选题申请
     */
    @PostMapping
    public ApiResponse<ApplicationResponse> submitApplication(
            @RequestBody ApplicationRequest requestDto,
            HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        
        // 目前匹配度先设为0.5，后续可以实现匹配算法计算
        BigDecimal matchScore = new BigDecimal("0.5");
        
        TopicApplication application = applicationService.submitApplication(
                requestDto.getTopicId(),
                studentId,
                requestDto.getRemark(),
                matchScore
        );
        
        ApplicationResponse response = convertToResponse(application);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取学生的申请列表
     */
    @GetMapping("/my")
    public ApiResponse<List<ApplicationResponse>> getMyApplications(HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        List<TopicApplication> applications = applicationService.getStudentApplications(studentId);
        
        List<ApplicationResponse> responses = applications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * 获取选题的申请列表（按匹配度排序，导师端）
     */
    @GetMapping("/topic/{topicId}")
    public ApiResponse<List<ApplicationResponse>> getTopicApplications(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "true") boolean sortByMatchScore) {
        List<TopicApplication> applications = applicationService.getTopicApplications(topicId, sortByMatchScore);
        
        List<ApplicationResponse> responses = applications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * 导师处理申请
     */
    @PostMapping("/{id}/process")
    public ApiResponse<Void> processApplication(
            @PathVariable Long id,
            @RequestBody ApplicationProcessRequest requestDto,
            HttpServletRequest request) {
        Long teacherId = getCurrentUserId(request);
        
        TopicApplication.ApplicationStatus status;
        try {
            status = TopicApplication.ApplicationStatus.valueOf(requestDto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的申请状态");
        }
        
        applicationService.processApplication(id, teacherId, status, requestDto.getFeedback());
        return ApiResponse.success(null);
    }
    
    /**
     * 转换TopicApplication实体为ApplicationResponse
     */
    private ApplicationResponse convertToResponse(TopicApplication application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setTopicId(application.getTopicId());
        response.setStudentId(application.getStudentId());
        response.setStatus(application.getStatus() != null ? application.getStatus().name() : null);
        response.setRemark(application.getRemark());
        response.setTeacherFeedback(application.getTeacherFeedback());
        response.setMatchScore(application.getMatchScore());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        
        // 获取选题标题
        if (application.getTopicId() != null) {
            Topic topic = topicMapper.selectById(application.getTopicId());
            if (topic != null) {
                response.setTopicTitle(topic.getTitle());
            }
        }
        
        // 获取学生姓名
        if (application.getStudentId() != null) {
            User student = userMapper.selectById(application.getStudentId());
            if (student != null) {
                response.setStudentName(student.getRealName());
            }
        }
        
        return response;
    }
}


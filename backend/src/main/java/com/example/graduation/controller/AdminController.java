package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.AdminPasswordRequest;
import com.example.graduation.dto.AdminResetPasswordRequest;
import com.example.graduation.dto.AdminUserListItem;
import com.example.graduation.dto.TopicReviewRequest;
import com.example.graduation.dto.TopicResponse;
import com.example.graduation.dto.UserCreateRequest;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicReview;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.TopicTagMapper;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.TopicService;
import com.example.graduation.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.TopicTag;
import com.example.graduation.entity.User.Role;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TopicService topicService;
    
    @Autowired
    private TopicTagMapper topicTagMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 创建用户（管理员功能）
     */
    @PostMapping("/users")
    public ApiResponse<User> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.createUser(request);
        // 不返回密码哈希
        user.setPasswordHash(null);
        return ApiResponse.success(user);
    }
    
    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 将 User 转为 AdminUserListItem，避免直接序列化实体导致闪退
     */
    private AdminUserListItem toListItem(User u) {
        AdminUserListItem item = new AdminUserListItem();
        item.setId(u.getId());
        item.setUsername(u.getUsername());
        item.setRealName(u.getRealName());
        item.setPasswordDisplay(u.getPasswordHash() != null && !u.getPasswordHash().isEmpty() ? "********" : "未设置");
        item.setRole(u.getRole() != null ? u.getRole().name() : "");
        item.setStatus(u.getStatus());
        item.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().format(ISO_DATETIME) : "");
        return item;
    }

    /**
     * 获取所有教师账号列表
     */
    @GetMapping("/users/teachers")
    public ApiResponse<List<AdminUserListItem>> listTeachers() {
        List<User> list = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, Role.TEACHER).orderByDesc(User::getCreatedAt)
        );
        List<AdminUserListItem> result = new ArrayList<>();
        for (User u : list) {
            result.add(toListItem(u));
        }
        return ApiResponse.success(result);
    }

    /**
     * 获取所有学生账号列表
     */
    @GetMapping("/users/students")
    public ApiResponse<List<AdminUserListItem>> listStudents() {
        List<User> list = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, Role.STUDENT).orderByDesc(User::getCreatedAt)
        );
        List<AdminUserListItem> result = new ArrayList<>();
        for (User u : list) {
            result.add(toListItem(u));
        }
        return ApiResponse.success(result);
    }

    /**
     * 管理员修改指定用户密码（POST + body，与批量重置一致，避免 PUT 预检问题）
     */
    @PostMapping("/users/change-password")
    public ApiResponse<Void> updateUserPassword(@Valid @RequestBody AdminPasswordRequest request) {
        userService.updatePassword(request.getUserId(), request.getNewPassword());
        return ApiResponse.success(null);
    }

    /**
     * 管理员批量重置密码为默认密码 123456
     */
    @PostMapping("/users/reset-password")
    public ApiResponse<Integer> resetPasswordsToDefault(@Valid @RequestBody AdminResetPasswordRequest request) {
        int count = userService.resetPasswordsToDefault(request.getUserIds());
        return ApiResponse.success(count);
    }
    
    /**
     * 获取待审核选题列表
     */
    @GetMapping("/topics/pending-review")
    public ApiResponse<List<TopicResponse>> getPendingReviewTopics() {
        List<Topic> topics = topicService.getTopics(Topic.TopicStatus.PENDING_REVIEW, null);
        List<TopicResponse> responses = convertToResponseList(topics);
        return ApiResponse.success(responses);
    }
    
    /**
     * 审核选题
     */
    @PostMapping("/topics/{id}/review")
    public ApiResponse<Void> reviewTopic(
            @PathVariable Long id,
            @RequestBody TopicReviewRequest requestDto,
            HttpServletRequest request) {
        Long adminId = getCurrentUserId(request);
        
        TopicReview.ReviewResult result;
        try {
            result = TopicReview.ReviewResult.valueOf(requestDto.getResult().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的审核结果");
        }
        
        topicService.reviewTopic(id, adminId, result, requestDto.getComment());
        return ApiResponse.success(null);
    }
    
    /**
     * 转换Topic实体为TopicResponse
     */
    private TopicResponse convertToResponse(Topic topic) {
        com.example.graduation.dto.TopicResponse response = new com.example.graduation.dto.TopicResponse();
        response.setId(topic.getId());
        response.setTeacherId(topic.getTeacherId());
        response.setTitle(topic.getTitle());
        response.setDescription(topic.getDescription());
        response.setStatus(topic.getStatus() != null ? topic.getStatus().name() : null);
        response.setMaxApplicants(topic.getMaxApplicants());
        response.setCurrentApplicants(topic.getCurrentApplicants());
        response.setCreatedAt(topic.getCreatedAt());
        response.setUpdatedAt(topic.getUpdatedAt());
        
        // 获取导师姓名
        if (topic.getTeacherId() != null) {
            User teacher = userMapper.selectById(topic.getTeacherId());
            if (teacher != null) {
                response.setTeacherName(teacher.getRealName());
            }
        }
        
        // 获取标签
        List<TopicTag> tags = topicTagMapper.selectList(
                new LambdaQueryWrapper<TopicTag>()
                        .eq(TopicTag::getTopicId, topic.getId())
        );
        List<String> tagNames = tags.stream()
                .map(TopicTag::getTagName)
                .collect(Collectors.toList());
        response.setTags(tagNames);
        
        return response;
    }
    
    /**
     * 转换Topic列表为TopicResponse列表
     */
    private List<TopicResponse> convertToResponseList(List<Topic> topics) {
        return topics.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}


package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.ChangePasswordRequest;
import com.example.graduation.dto.TagRegenerateRequest;
import com.example.graduation.dto.TagRegenerateTaskCreateResponse;
import com.example.graduation.dto.TagRegenerateTaskStatusResponse;
import com.example.graduation.dto.TeacherProfileRequest;
import com.example.graduation.dto.TeacherProfileResponse;
import com.example.graduation.dto.UserTagResponse;
import com.example.graduation.entity.TeacherProfile;
import com.example.graduation.entity.User;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.TagRegenerateTaskService;
import com.example.graduation.service.TeacherService;
import com.example.graduation.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    
    @Autowired
    private TeacherService teacherService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private TagRegenerateTaskService tagRegenerateTaskService;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 获取导师个人信息
     */
    @GetMapping("/profile")
    public ApiResponse<TeacherProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        TeacherProfile profile = teacherService.getProfile(userId);
        List<UserTag> tags = teacherService.getTags(userId);
        
        TeacherProfileResponse response = new TeacherProfileResponse();
        response.setUserId(userId);

        User user = userMapper.selectById(userId);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setRealName(user.getRealName());
        }
        if (profile != null) {
            response.setTitle(profile.getTitle());
            response.setResearchDirection(profile.getResearchDirection());
            response.setMaxStudentCount(profile.getMaxStudentCount());
        }
        
        List<UserTagResponse> tagResponses = tags.stream().map(tag -> {
            UserTagResponse tagResponse = new UserTagResponse();
            tagResponse.setTagName(tag.getTagName());
            tagResponse.setTagType(tag.getTagType());
            tagResponse.setWeight(tag.getWeight());
            return tagResponse;
        }).collect(Collectors.toList());
        response.setTags(tagResponses);
        
        return ApiResponse.success(response);
    }
    
    /**
     * 更新导师个人信息
     */
    @PutMapping("/profile")
    public ApiResponse<TeacherProfileResponse> updateProfile(
            @RequestBody TeacherProfileRequest requestDto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        TeacherProfile profile = teacherService.updateProfile(
                userId,
                requestDto.getRealName(),
                requestDto.getTitle(),
                requestDto.getResearchDirection(),
                requestDto.getMaxStudentCount()
        );
        
        List<UserTag> tags = teacherService.getTags(userId);
        
        TeacherProfileResponse response = new TeacherProfileResponse();
        response.setUserId(userId);
        User user = userMapper.selectById(userId);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setRealName(user.getRealName());
        }
        response.setTitle(profile.getTitle());
        response.setResearchDirection(profile.getResearchDirection());
        response.setMaxStudentCount(profile.getMaxStudentCount());
        
        List<UserTagResponse> tagResponses = tags.stream().map(tag -> {
            UserTagResponse tagResponse = new UserTagResponse();
            tagResponse.setTagName(tag.getTagName());
            tagResponse.setTagType(tag.getTagType());
            tagResponse.setWeight(tag.getWeight());
            return tagResponse;
        }).collect(Collectors.toList());
        response.setTags(tagResponses);
        
        return ApiResponse.success(response);
    }
    
    /**
     * 获取导师标签
     */
    @GetMapping("/tags")
    public ApiResponse<List<UserTagResponse>> getTags(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<UserTag> tags = teacherService.getTags(userId);
        
        List<UserTagResponse> responses = tags.stream().map(tag -> {
            UserTagResponse response = new UserTagResponse();
            response.setTagName(tag.getTagName());
            response.setTagType(tag.getTagType());
            response.setWeight(tag.getWeight());
            return response;
        }).collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }

    /**
     * 保存（覆盖）导师标签：用于前端手动编辑/删除后落库。
     */
    @PutMapping("/tags")
    public ApiResponse<List<UserTagResponse>> updateTags(
            @RequestBody List<UserTagResponse> dtoTags,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        List<UserTag> tags = new java.util.ArrayList<>();
        if (dtoTags != null) {
            for (UserTagResponse t : dtoTags) {
                if (t == null || t.getTagName() == null || t.getTagName().trim().isEmpty()) {
                    continue;
                }
                UserTag tag = new UserTag();
                tag.setTagName(t.getTagName().trim());
                tag.setTagType(t.getTagType());
                java.math.BigDecimal w = t.getWeight() == null ? new java.math.BigDecimal("0.90") : t.getWeight();
                if (w.compareTo(java.math.BigDecimal.ZERO) < 0) w = java.math.BigDecimal.ZERO;
                if (w.compareTo(java.math.BigDecimal.ONE) > 0) w = java.math.BigDecimal.ONE;
                tag.setWeight(w);
                tags.add(tag);
            }
        }
        teacherService.updateTags(userId, tags);

        List<UserTagResponse> responses = tags.stream().map(tag -> {
            UserTagResponse response = new UserTagResponse();
            response.setTagName(tag.getTagName());
            response.setTagType(tag.getTagType());
            response.setWeight(tag.getWeight());
            return response;
        }).collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    /**
     * 交互式“重抽标签”：保留 pinnedTags，并让模型不要重复生成 excludeTagNames（固定的除外）
     * 对导师来说，文本来源是研究方向。
     */
    @PostMapping("/tags/regenerate")
    public ApiResponse<TagRegenerateTaskCreateResponse> regenerateTags(
            @RequestBody TagRegenerateRequest dto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        String taskId = tagRegenerateTaskService.submit("teacher:" + userId, () ->
                teacherService.regenerateTags(
                        userId,
                        dto.getInterestDesc(), // 前端传入研究方向文本
                        dto.getPinnedTags(),
                        dto.getExcludeTagNames(),
                        dto.getDesiredTotal()
                )
        );

        return ApiResponse.success(new TagRegenerateTaskCreateResponse(taskId));
    }

    @GetMapping("/tags/regenerate/{taskId}")
    public ApiResponse<TagRegenerateTaskStatusResponse> getRegenerateStatus(@PathVariable String taskId) {
        return ApiResponse.success(tagRegenerateTaskService.getStatus(taskId));
    }

    /**
     * 导师修改自己的登录密码
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest dto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        userService.changeOwnPassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return ApiResponse.success(null);
    }
}


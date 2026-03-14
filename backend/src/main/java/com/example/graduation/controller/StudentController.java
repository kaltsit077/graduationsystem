package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.ChangePasswordRequest;
import com.example.graduation.dto.StudentProfileRequest;
import com.example.graduation.dto.StudentProfileResponse;
import com.example.graduation.dto.TagRegenerateRequest;
import com.example.graduation.dto.UserTagResponse;
import com.example.graduation.entity.StudentProfile;
import com.example.graduation.entity.User;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.StudentService;
import com.example.graduation.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    
    @Autowired
    private StudentService studentService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 获取学生个人信息
     */
    @GetMapping("/profile")
    public ApiResponse<StudentProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        StudentProfile profile = studentService.getProfile(userId);
        List<UserTag> tags = studentService.getTags(userId);
        
        StudentProfileResponse response = new StudentProfileResponse();
        response.setUserId(userId);

        User user = userMapper.selectById(userId);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setRealName(user.getRealName());
        }
        if (profile != null) {
            response.setMajor(profile.getMajor());
            response.setGrade(profile.getGrade());
            response.setInterestDesc(profile.getInterestDesc());
            response.setTagMode(profile.getTagMode());
        }
        
        List<UserTagResponse> tagResponses = tags.stream().map(tag -> {
            UserTagResponse tagResponse = new UserTagResponse();
            tagResponse.setTagName(tag.getTagName());
            tagResponse.setWeight(tag.getWeight());
            return tagResponse;
        }).collect(Collectors.toList());
        response.setTags(tagResponses);
        
        return ApiResponse.success(response);
    }
    
    /**
     * 更新学生个人信息
     */
    @PutMapping("/profile")
    public ApiResponse<StudentProfileResponse> updateProfile(
            @RequestBody StudentProfileRequest requestDto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        StudentProfile profile = studentService.updateProfile(
                userId,
                requestDto.getRealName(),
                requestDto.getMajor(),
                requestDto.getGrade(),
                requestDto.getInterestDesc(),
                requestDto.getTagMode()
        );
        
        List<UserTag> tags = studentService.getTags(userId);
        
        StudentProfileResponse response = new StudentProfileResponse();
        response.setUserId(userId);
        User user = userMapper.selectById(userId);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setRealName(user.getRealName());
        }
        response.setMajor(profile.getMajor());
        response.setGrade(profile.getGrade());
        response.setInterestDesc(profile.getInterestDesc());
        response.setTagMode(profile.getTagMode());
        
        List<UserTagResponse> tagResponses = tags.stream().map(tag -> {
            UserTagResponse tagResponse = new UserTagResponse();
            tagResponse.setTagName(tag.getTagName());
            tagResponse.setWeight(tag.getWeight());
            return tagResponse;
        }).collect(Collectors.toList());
        response.setTags(tagResponses);
        
        return ApiResponse.success(response);
    }

    /**
     * 学生修改自己的登录密码
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest dto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        userService.changeOwnPassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return ApiResponse.success(null);
    }
    
    /**
     * 获取学生标签
     */
    @GetMapping("/tags")
    public ApiResponse<List<UserTagResponse>> getTags(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<UserTag> tags = studentService.getTags(userId);
        
        List<UserTagResponse> responses = tags.stream().map(tag -> {
            UserTagResponse response = new UserTagResponse();
            response.setTagName(tag.getTagName());
            response.setWeight(tag.getWeight());
            return response;
        }).collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }

    /**
     * 保存（覆盖）学生标签：用于前端手动编辑/删除后落库。
     */
    @PutMapping("/tags")
    public ApiResponse<List<UserTagResponse>> updateTags(
            @RequestBody List<UserTagResponse> dtoTags,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        List<UserTag> tags = new ArrayList<>();
        if (dtoTags != null) {
            for (UserTagResponse t : dtoTags) {
                if (t == null || t.getTagName() == null || t.getTagName().trim().isEmpty()) {
                    continue;
                }
                UserTag tag = new UserTag();
                tag.setTagName(t.getTagName().trim());
                tag.setWeight(t.getWeight() == null ? new BigDecimal("0.90") : t.getWeight());
                tags.add(tag);
            }
        }
        studentService.updateTags(userId, tags);

        List<UserTagResponse> responses = tags.stream().map(tag -> {
            UserTagResponse response = new UserTagResponse();
            response.setTagName(tag.getTagName());
            response.setWeight(tag.getWeight());
            return response;
        }).collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    /**
     * 交互式“重抽标签”：保留 pinnedTags，并让模型不要重复生成 excludeTagNames（固定的除外）
     */
    @PostMapping("/tags/regenerate")
    public ApiResponse<List<UserTagResponse>> regenerateTags(
            @RequestBody TagRegenerateRequest dto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        List<UserTag> tags = studentService.regenerateTags(
                userId,
                dto.getInterestDesc(),
                dto.getMajor(),
                dto.getTagMode(),
                dto.getPinnedTags(),
                dto.getExcludeTagNames(),
                dto.getDesiredTotal()
        );

        List<UserTagResponse> responses = tags.stream().map(tag -> {
            UserTagResponse response = new UserTagResponse();
            response.setTagName(tag.getTagName());
            response.setWeight(tag.getWeight());
            return response;
        }).collect(Collectors.toList());

        return ApiResponse.success(responses);
    }
}


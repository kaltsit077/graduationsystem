package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.TeacherProfileRequest;
import com.example.graduation.dto.TeacherProfileResponse;
import com.example.graduation.dto.UserTagResponse;
import com.example.graduation.entity.TeacherProfile;
import com.example.graduation.entity.User;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.TeacherService;
import jakarta.servlet.http.HttpServletRequest;
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
            response.setWeight(tag.getWeight());
            return response;
        }).collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
}


package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.StudentProfileRequest;
import com.example.graduation.dto.StudentProfileResponse;
import com.example.graduation.dto.UserTagResponse;
import com.example.graduation.entity.StudentProfile;
import com.example.graduation.entity.UserTag;
import com.example.graduation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    
    @Autowired
    private StudentService studentService;
    
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
        if (profile != null) {
            response.setUserId(profile.getUserId());
            response.setMajor(profile.getMajor());
            response.setGrade(profile.getGrade());
            response.setInterestDesc(profile.getInterestDesc());
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
                requestDto.getMajor(),
                requestDto.getGrade(),
                requestDto.getInterestDesc()
        );
        
        List<UserTag> tags = studentService.getTags(userId);
        
        StudentProfileResponse response = new StudentProfileResponse();
        response.setUserId(profile.getUserId());
        response.setMajor(profile.getMajor());
        response.setGrade(profile.getGrade());
        response.setInterestDesc(profile.getInterestDesc());
        
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
}


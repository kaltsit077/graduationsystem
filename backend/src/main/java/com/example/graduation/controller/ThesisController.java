package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.ThesisResponse;
import com.example.graduation.dto.ThesisUploadRequest;
import com.example.graduation.entity.Thesis;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.ThesisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thesis")
public class ThesisController {
    
    @Autowired
    private ThesisService thesisService;
    
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
     * 上传论文
     */
    @PostMapping("/upload")
    public ApiResponse<ThesisResponse> uploadThesis(
            @RequestBody ThesisUploadRequest requestDto,
            HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        
        Thesis thesis = thesisService.uploadThesis(
                requestDto.getTopicId(),
                studentId,
                requestDto.getFileUrl(),
                requestDto.getFileName(),
                requestDto.getFileSize()
        );
        
        ThesisResponse response = convertToResponse(thesis);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取学生的论文列表
     */
    @GetMapping("/my")
    public ApiResponse<List<ThesisResponse>> getMyTheses(HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        List<Thesis> theses = thesisService.getStudentTheses(studentId);
        
        List<ThesisResponse> responses = theses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * 获取导师的论文列表
     */
    @GetMapping("/teacher")
    public ApiResponse<List<ThesisResponse>> getTeacherTheses(HttpServletRequest request) {
        Long teacherId = getCurrentUserId(request);
        List<Thesis> theses = thesisService.getTeacherTheses(teacherId);
        
        List<ThesisResponse> responses = theses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * 获取论文详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ThesisResponse> getThesis(@PathVariable Long id) {
        Thesis thesis = thesisService.getThesis(id);
        if (thesis == null) {
            return ApiResponse.error("论文不存在");
        }
        
        ThesisResponse response = convertToResponse(thesis);
        return ApiResponse.success(response);
    }
    
    /**
     * 转换Thesis实体为ThesisResponse
     */
    private ThesisResponse convertToResponse(Thesis thesis) {
        ThesisResponse response = new ThesisResponse();
        response.setId(thesis.getId());
        response.setTopicId(thesis.getTopicId());
        response.setStudentId(thesis.getStudentId());
        response.setFileUrl(thesis.getFileUrl());
        response.setFileName(thesis.getFileName());
        response.setFileSize(thesis.getFileSize());
        response.setStatus(thesis.getStatus() != null ? thesis.getStatus().name() : null);
        response.setCreatedAt(thesis.getCreatedAt());
        response.setUpdatedAt(thesis.getUpdatedAt());
        
        // 获取选题标题
        if (thesis.getTopicId() != null) {
            Topic topic = topicMapper.selectById(thesis.getTopicId());
            if (topic != null) {
                response.setTopicTitle(topic.getTitle());
            }
        }
        
        // 获取学生姓名
        if (thesis.getStudentId() != null) {
            User student = userMapper.selectById(thesis.getStudentId());
            if (student != null) {
                response.setStudentName(student.getRealName());
            }
        }
        
        return response;
    }
}


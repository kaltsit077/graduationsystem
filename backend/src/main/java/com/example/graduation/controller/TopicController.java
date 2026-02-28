package com.example.graduation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.TopicDuplicateCheckRequest;
import com.example.graduation.dto.TopicDuplicateCheckResponse;
import com.example.graduation.dto.TopicRequest;
import com.example.graduation.dto.TopicResponse;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicTag;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.TopicTagMapper;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.TopicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    
    @Autowired
    private TopicService topicService;
    
    @Autowired
    private TopicMapper topicMapper;
    
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
     * 获取已开放的选题列表（学生端）
     */
    @GetMapping("/open")
    public ApiResponse<List<TopicResponse>> getOpenTopics() {
        List<Topic> topics = topicService.getOpenTopics();
        List<TopicResponse> responses = convertToResponseList(topics);
        return ApiResponse.success(responses);
    }
    
    /**
     * 获取选题列表（可按状态筛选，导师端）
     */
    @GetMapping
    public ApiResponse<List<TopicResponse>> getTopics(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teacherId,
            HttpServletRequest request) {
        Topic.TopicStatus topicStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                topicStatus = Topic.TopicStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 忽略无效的状态值
            }
        }
        
        Long queryTeacherId = teacherId;
        if (queryTeacherId == null) {
            // 如果没有指定teacherId，且当前用户是导师，则查询当前导师的选题
            String role = (String) request.getAttribute("role");
            if ("TEACHER".equals(role)) {
                queryTeacherId = getCurrentUserId(request);
            }
        }
        
        List<Topic> topics = topicService.getTopics(topicStatus, queryTeacherId);
        List<TopicResponse> responses = convertToResponseList(topics);
        return ApiResponse.success(responses);
    }
    
    /**
     * 获取选题详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TopicResponse> getTopic(@PathVariable Long id) {
        Topic topic = topicMapper.selectById(id);
        if (topic == null) {
            return ApiResponse.error("选题不存在");
        }
        
        TopicResponse response = convertToResponse(topic);
        return ApiResponse.success(response);
    }
    
    /**
     * 创建选题（导师端）
     */
    @PostMapping
    public ApiResponse<TopicResponse> createTopic(
            @Valid @RequestBody TopicRequest requestDto,
            HttpServletRequest request) {
        Long teacherId = getCurrentUserId(request);
        
        Topic topic = new Topic();
        topic.setTeacherId(teacherId);
        topic.setTitle(requestDto.getTitle());
        topic.setDescription(requestDto.getDescription());
        topic.setMaxApplicants(requestDto.getMaxApplicants());
        
        Topic created = topicService.createTopic(topic, requestDto.getTags());
        TopicResponse response = convertToResponse(created);
        return ApiResponse.success(response);
    }
    
    /**
     * 更新选题（导师端）
     */
    @PutMapping("/{id}")
    public ApiResponse<TopicResponse> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody TopicRequest requestDto,
            HttpServletRequest request) {
        // 注意：权限验证在 TopicService.updateTopic() 中完成
        
        Topic topic = new Topic();
        topic.setTitle(requestDto.getTitle());
        topic.setDescription(requestDto.getDescription());
        topic.setMaxApplicants(requestDto.getMaxApplicants());
        
        Topic updated = topicService.updateTopic(id, topic, requestDto.getTags());
        TopicResponse response = convertToResponse(updated);
        return ApiResponse.success(response);
    }
    
    /**
     * 去重检测
     */
    @PostMapping("/check-duplicate")
    public ApiResponse<TopicDuplicateCheckResponse> checkDuplicate(
            @RequestBody TopicDuplicateCheckRequest requestDto) {
        TopicService.DuplicateCheckResult result = topicService.checkDuplicate(
                requestDto.getTopicId(),
                requestDto.getTitle(),
                requestDto.getDescription()
        );
        
        TopicDuplicateCheckResponse response = new TopicDuplicateCheckResponse();
        response.setPassed(result.isPassed());
        response.setMaxSimilarity(result.getMaxSimilarity());
        response.setSimilarTopicId(result.getSimilarTopicId());
        response.setSimilarTopicTitle(result.getSimilarTopicTitle());
        
        return ApiResponse.success(response);
    }
    
    /**
     * 提交选题审核（导师端）
     */
    @PostMapping("/{id}/submit-review")
    public ApiResponse<Void> submitForReview(@PathVariable Long id) {
        topicService.submitForReview(id);
        return ApiResponse.success(null);
    }
    
    /**
     * 转换Topic实体为TopicResponse
     */
    private TopicResponse convertToResponse(Topic topic) {
        TopicResponse response = new TopicResponse();
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


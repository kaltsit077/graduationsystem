package com.example.graduation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.ThesisEvaluationRequest;
import com.example.graduation.dto.TopicMetricsResponse;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicMetrics;
import com.example.graduation.entity.ThesisEvaluation;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.TopicMetricsMapper;
import com.example.graduation.mapper.ThesisEvaluationMapper;
import com.example.graduation.service.EvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eval")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private TopicMetricsMapper topicMetricsMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private ThesisEvaluationMapper thesisEvaluationMapper;

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 教师录入或更新某篇论文的评价
     */
    @PostMapping("/thesis")
    public ApiResponse<Void> saveThesisEvaluation(
            @Valid @RequestBody ThesisEvaluationRequest requestDto,
            HttpServletRequest request) {
        Long teacherId = getCurrentUserId(request);
        evaluationService.saveEvaluation(
                teacherId,
                requestDto.getThesisId(),
                requestDto.getScore(),
                requestDto.getDefenseScore(),
                requestDto.getReviewScore(),
                requestDto.getGradeLevel(),
                requestDto.getComment()
        );
        // 每次评价后，顺便重算该导师的指标
        evaluationService.recalcTopicMetricsForTeacher(teacherId);
        return ApiResponse.success(null);
    }

    /**
     * 学生录入或更新对论文/导师的评价
     */
    @PostMapping("/thesis/student")
    public ApiResponse<Void> saveStudentThesisFeedback(
            @Valid @RequestBody ThesisEvaluationRequest requestDto,
            HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        evaluationService.saveStudentFeedback(
                studentId,
                requestDto.getThesisId(),
                requestDto.getScore(),
                requestDto.getComment()
        );
        return ApiResponse.success(null);
    }

    /**
     * 获取某篇论文的评价详情（含教师评分和学生反馈）
     */
    @GetMapping("/thesis/{thesisId}")
    public ApiResponse<ThesisEvaluation> getThesisEvaluation(@PathVariable Long thesisId) {
        ThesisEvaluation eval = thesisEvaluationMapper.selectOne(
                new LambdaQueryWrapper<ThesisEvaluation>()
                        .eq(ThesisEvaluation::getThesisId, thesisId)
        );
        return ApiResponse.success(eval);
    }

    /**
     * 导师查看自己所有选题的质量统计（用于可视化）
     */
    @GetMapping("/teacher/topics")
    public ApiResponse<List<TopicMetricsResponse>> getTeacherTopicMetrics(HttpServletRequest request) {
        Long teacherId = getCurrentUserId(request);
        List<TopicMetrics> metricsList = topicMetricsMapper.selectList(
                new LambdaQueryWrapper<TopicMetrics>()
                        .eq(TopicMetrics::getTeacherId, teacherId)
        );
        List<TopicMetricsResponse> responses = metricsList.stream().map(metrics -> {
            TopicMetricsResponse dto = new TopicMetricsResponse();
            dto.setTopicId(metrics.getTopicId());
            dto.setAvgScore(metrics.getAvgScore());
            dto.setTotalStudents(metrics.getTotalStudents());
            dto.setExcellentRatio(metrics.getExcellentRatio());
            dto.setFailRatio(metrics.getFailRatio());
            Topic topic = topicMapper.selectById(metrics.getTopicId());
            dto.setTopicTitle(topic != null ? topic.getTitle() : "");
            return dto;
        }).collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    /**
     * 管理员查看全局选题质量统计（按选题聚合）
     */
    @GetMapping("/admin/topics")
    public ApiResponse<List<TopicMetricsResponse>> getAllTopicMetrics() {
        List<TopicMetrics> metricsList = topicMetricsMapper.selectList(null);
        List<TopicMetricsResponse> responses = metricsList.stream().map(metrics -> {
            TopicMetricsResponse dto = new TopicMetricsResponse();
            dto.setTopicId(metrics.getTopicId());
            dto.setAvgScore(metrics.getAvgScore());
            dto.setTotalStudents(metrics.getTotalStudents());
            dto.setExcellentRatio(metrics.getExcellentRatio());
            dto.setFailRatio(metrics.getFailRatio());
            Topic topic = topicMapper.selectById(metrics.getTopicId());
            dto.setTopicTitle(topic != null ? topic.getTitle() : "");
            return dto;
        }).collect(Collectors.toList());
        return ApiResponse.success(responses);
    }
}


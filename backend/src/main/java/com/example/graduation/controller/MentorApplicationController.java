package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.MentorApplicationCreateRequest;
import com.example.graduation.dto.MentorApplicationResponse;
import com.example.graduation.dto.TeacherOverviewResponse;
import com.example.graduation.entity.MentorApplication;
import com.example.graduation.service.MentorApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mentor-applications")
public class MentorApplicationController {

    @Autowired
    private MentorApplicationService mentorApplicationService;

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : null;
    }

    /**
     * 学生发起拜师申请
     */
    @PostMapping
    public ApiResponse<MentorApplicationResponse> createMentorApplication(
            @RequestBody MentorApplicationCreateRequest requestDto,
            HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"STUDENT".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅学生可以发起拜师申请");
        }
        Long studentId = getCurrentUserId(request);
        MentorApplication app = mentorApplicationService.createMentorApplication(
                studentId,
                requestDto.getTeacherId(),
                requestDto.getReason()
        );
        return ApiResponse.success(mentorApplicationService.toResponse(app));
    }

    /**
     * 学生查看自己的拜师申请
     */
    @GetMapping("/my")
    public ApiResponse<List<MentorApplicationResponse>> getMyMentorApplications(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"STUDENT".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅学生可以查看该列表");
        }
        Long studentId = getCurrentUserId(request);
        List<MentorApplication> list = mentorApplicationService.getStudentApplications(studentId);
        List<MentorApplicationResponse> responses = list.stream()
                .map(mentorApplicationService::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    /**
     * 导师查看名下待处理的拜师申请
     */
    @GetMapping("/teacher/pending")
    public ApiResponse<List<MentorApplicationResponse>> getTeacherPendingApplications(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"TEACHER".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅导师可以查看该列表");
        }
        Long teacherId = getCurrentUserId(request);
        List<MentorApplication> list = mentorApplicationService.getTeacherPendingApplications(teacherId);
        List<MentorApplicationResponse> responses = list.stream()
                .map(mentorApplicationService::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    /**
     * 导师审批拜师申请
     */
    @PostMapping("/{id}/decision")
    public ApiResponse<Void> handleMentorApplicationDecision(
            @PathVariable Long id,
            @RequestParam("status") MentorApplication.Status status,
            @RequestParam(value = "comment", required = false) String comment,
            HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"TEACHER".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅导师可以执行该操作");
        }
        Long teacherId = getCurrentUserId(request);
        mentorApplicationService.handleMentorApplicationDecision(id, teacherId, status, comment);
        return ApiResponse.success(null);
    }

    /**
     * 导师为已通过的拜师申请指派课题（从已有题目中选择）
     */
    @PostMapping("/{id}/assign-topic")
    public ApiResponse<Void> assignTopicForApplication(
            @PathVariable Long id,
            @RequestParam("topicId") Long topicId,
            HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"TEACHER".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅导师可以执行该操作");
        }
        Long teacherId = getCurrentUserId(request);
        mentorApplicationService.assignTopicForApplication(id, teacherId, topicId);
        return ApiResponse.success(null);
    }

    /**
     * 学生端导师列表概览
     */
    @GetMapping("/teachers/overview")
    public ApiResponse<List<TeacherOverviewResponse>> getTeacherOverviewList(HttpServletRequest request) {
        // 只要求已登录，角色不限（学生用得最多）
        String role = getCurrentUserRole(request);
        if (role == null) {
            return ApiResponse.error("未登录");
        }
        List<TeacherOverviewResponse> list = mentorApplicationService.getTeacherOverviewList();
        return ApiResponse.success(list);
    }
}


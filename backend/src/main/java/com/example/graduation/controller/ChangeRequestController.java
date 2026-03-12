package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.ChangeRequestCreateRequest;
import com.example.graduation.dto.ChangeRequestResponse;
import com.example.graduation.entity.ChangeRequest;
import com.example.graduation.service.ChangeRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/change-requests")
public class ChangeRequestController {

    @Autowired
    private ChangeRequestService changeRequestService;

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 获取当前用户角色（STUDENT / TEACHER / ADMIN）
     */
    private String getCurrentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : null;
    }

    /**
     * 学生发起选题 / 导师变更申请
     */
    @PostMapping
    public ApiResponse<ChangeRequestResponse> createChangeRequest(
            @RequestBody ChangeRequestCreateRequest requestDto,
            HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        ChangeRequest changeRequest = changeRequestService.createChangeRequest(
                studentId,
                requestDto.getType(),
                requestDto.getReason(),
                requestDto.getTargetTopicId(),
                requestDto.getTargetTeacherId()
        );
        return ApiResponse.success(convertToResponse(changeRequest));
    }

    /**
     * 学生查看自己的变更申请列表
     */
    @GetMapping("/my")
    public ApiResponse<List<ChangeRequestResponse>> getMyChangeRequests(HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        List<ChangeRequest> list = changeRequestService.getStudentRequests(studentId);
        List<ChangeRequestResponse> responses = list.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    /**
     * 导师查看名下待处理的“更换选题”申请
     */
    @GetMapping("/teacher/pending")
    public ApiResponse<List<ChangeRequestResponse>> getTeacherPending(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"TEACHER".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅导师可以查看该列表");
        }
        Long teacherId = getCurrentUserId(request);
        List<ChangeRequest> list = changeRequestService.getTeacherPendingRequests(teacherId);
        List<ChangeRequestResponse> responses = list.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    /**
     * 导师审批更换选题申请
     */
    @PostMapping("/{id}/teacher-decision")
    public ApiResponse<Void> handleTeacherDecision(
            @PathVariable Long id,
            @RequestParam ChangeRequest.Decision decision,
            @RequestParam(required = false) String comment,
            HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"TEACHER".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅导师可以执行该操作");
        }
        Long teacherId = getCurrentUserId(request);
        changeRequestService.handleTeacherDecision(id, teacherId, decision, comment);
        return ApiResponse.success(null);
    }

    /**
     * 管理员查看待处理的更换导师申请
     */
    @GetMapping("/admin/pending")
    public ApiResponse<List<ChangeRequestResponse>> getAdminPending(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅管理员可以查看该列表");
        }
        List<ChangeRequest> list = changeRequestService.getAdminPendingRequests();
        List<ChangeRequestResponse> responses = list.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    /**
     * 管理员审批更换导师申请
     */
    @PostMapping("/{id}/admin-decision")
    public ApiResponse<Void> handleAdminDecision(
            @PathVariable Long id,
            @RequestParam ChangeRequest.Decision decision,
            @RequestParam(required = false) String comment,
            HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ApiResponse.error("仅管理员可以执行该操作");
        }
        changeRequestService.handleAdminDecision(id, decision, comment);
        return ApiResponse.success(null);
    }

    private ChangeRequestResponse convertToResponse(ChangeRequest changeRequest) {
        ChangeRequestResponse resp = new ChangeRequestResponse();
        resp.setId(changeRequest.getId());
        resp.setStudentId(changeRequest.getStudentId());
        resp.setCurrentApplicationId(changeRequest.getCurrentApplicationId());
        resp.setType(changeRequest.getType());
        resp.setReason(changeRequest.getReason());
        resp.setTargetTopicId(changeRequest.getTargetTopicId());
        resp.setTargetTeacherId(changeRequest.getTargetTeacherId());
        resp.setStatus(changeRequest.getStatus());
        resp.setTeacherDecision(changeRequest.getTeacherDecision());
        resp.setTeacherComment(changeRequest.getTeacherComment());
        resp.setAdminDecision(changeRequest.getAdminDecision());
        resp.setAdminComment(changeRequest.getAdminComment());
        resp.setCreatedAt(changeRequest.getCreatedAt());
        resp.setUpdatedAt(changeRequest.getUpdatedAt());
        return resp;
    }
}


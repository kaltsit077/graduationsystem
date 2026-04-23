package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.CollabStageProgressItemResponse;
import com.example.graduation.dto.CollabStageWindowItemRequest;
import com.example.graduation.service.CollabService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/collab")
public class CollabController {

    @Autowired
    private CollabService collabService;

    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : null;
    }

    @GetMapping("/applications/{applicationId}/progress")
    public ApiResponse<List<CollabStageProgressItemResponse>> progress(
            @PathVariable Long applicationId,
            HttpServletRequest request
    ) {
        List<CollabStageProgressItemResponse> list = collabService.buildProgress(
                applicationId,
                getCurrentUserId(request),
                getCurrentUserRole(request)
        );
        return ApiResponse.success(list);
    }

    @PutMapping("/applications/{applicationId}/stage-windows")
    public ApiResponse<Void> saveWindows(
            @PathVariable Long applicationId,
            @RequestBody(required = false) List<CollabStageWindowItemRequest> body,
            HttpServletRequest request
    ) {
        List<CollabStageWindowItemRequest> list = body != null ? body : Collections.emptyList();
        collabService.saveStageWindows(
                applicationId,
                getCurrentUserId(request),
                getCurrentUserRole(request),
                list
        );
        return ApiResponse.success(null);
    }
}

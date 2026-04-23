package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.ThesisResponse;
import com.example.graduation.dto.ThesisUploadRequest;
import com.example.graduation.dto.ThesisWorkflowRequest;
import com.example.graduation.entity.CollabStage;
import com.example.graduation.entity.Thesis;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.ThesisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
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

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private static final long MAX_BYTES = 30L * 1024 * 1024; // 30MB
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "ppt", "pptx", "zip", "rar", "7z");
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : "";
    }

    private static CollabStage resolveUploadStage(String raw) {
        if (raw == null || raw.isBlank()) {
            return CollabStage.PRE_DEFENSE_THESIS;
        }
        return CollabStage.fromCode(raw);
    }
    
    /**
     * 上传论文
     */
    @PostMapping("/upload")
    public ApiResponse<ThesisResponse> uploadThesis(
            @RequestBody ThesisUploadRequest requestDto,
            HttpServletRequest request) {
        Long studentId = getCurrentUserId(request);
        CollabStage stage = resolveUploadStage(requestDto.getStage());

        Thesis thesis = thesisService.uploadThesis(
                requestDto.getTopicId(),
                studentId,
                requestDto.getFileUrl(),
                requestDto.getFileName(),
                requestDto.getFileSize(),
                stage
        );
        
        ThesisResponse response = convertToResponse(thesis);
        return ApiResponse.success(response);
    }

    /**
     * 上传论文文件（服务器存储），返回可直接访问的 URL
     */
    @PostMapping("/upload-file")
    public ApiResponse<ThesisResponse> uploadThesisFile(
            @RequestParam("topicId") Long topicId,
            @RequestParam(value = "stage", required = false) String stageParam,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) throws IOException {
        Long studentId = getCurrentUserId(request);
        CollabStage stage = resolveUploadStage(stageParam);

        if (topicId == null) {
            throw new RuntimeException("topicId 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new RuntimeException("文件过大，请上传 30MB 以内的文件");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new RuntimeException("仅支持 pdf/doc/docx/ppt/pptx/zip/rar/7z 格式");
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path baseThesis = root.resolve("thesis");
        Path dir = baseThesis
                .resolve("student-" + studentId)
                .resolve("topic-" + topicId)
                .resolve(stage.name());
        Files.createDirectories(dir);

        String filename = stage.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(baseThesis)) {
            throw new RuntimeException("非法文件路径");
        }
        file.transferTo(target.toFile());

        String url = "/uploads/thesis/student-" + studentId + "/topic-" + topicId + "/" + stage.name() + "/" + filename;
        String fileName = original == null || original.trim().isEmpty() ? filename : original.trim();
        long fileSize = file.getSize();

        Thesis thesis = thesisService.uploadThesis(
                topicId,
                studentId,
                url,
                fileName,
                fileSize,
                stage
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
     * 导师/管理员：环节稿件审核（通过 / 退回修改）
     */
    @PostMapping("/{id}/workflow")
    public ApiResponse<Void> workflow(
            @PathVariable Long id,
            @RequestBody ThesisWorkflowRequest body,
            HttpServletRequest request
    ) {
        if (body.getDecision() == null) {
            return ApiResponse.error("缺少 decision");
        }
        String d = body.getDecision().trim().toUpperCase();
        boolean approve = "APPROVE".equals(d) || "APPROVED".equals(d);
        boolean reject = "NEED_REVISION".equals(d) || "REJECT".equals(d);
        if (!approve && !reject) {
            return ApiResponse.error("decision 仅支持 APPROVE 或 NEED_REVISION");
        }
        thesisService.reviewThesisWorkflow(id, getCurrentUserId(request), getCurrentUserRole(request), approve);
        return ApiResponse.success(null);
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
        response.setStage(thesis.getStage());
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


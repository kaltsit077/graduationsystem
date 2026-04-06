package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.AdminDeleteUsersRequest;
import com.example.graduation.dto.AdminPasswordRequest;
import com.example.graduation.dto.AdminResetPasswordRequest;
import com.example.graduation.dto.AdminUserListItem;
import com.example.graduation.dto.MonitorStatusResponse;
import com.example.graduation.dto.SelectionSettingRequest;
import com.example.graduation.dto.SelectionSettingResponse;
import com.example.graduation.dto.TeacherLoadItemResponse;
import com.example.graduation.dto.TopicReviewRequest;
import com.example.graduation.dto.TopicResponse;
import com.example.graduation.dto.UserCreateRequest;
import com.example.graduation.entity.SystemSetting;
import com.example.graduation.entity.TeacherProfile;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicReview;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.TeacherProfileMapper;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.TopicTagMapper;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.service.SystemSettingService;
import com.example.graduation.service.TopicService;
import com.example.graduation.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.TopicTag;
import com.example.graduation.entity.User.Role;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariDataSource;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TopicService topicService;
    
    @Autowired
    private TopicTagMapper topicTagMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    @Autowired
    private TopicApplicationMapper topicApplicationMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SystemSettingService systemSettingService;

    @Value("${logging.file.name:logs/graduation-backend.log}")
    private String logFilePath;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
    
    /**
     * 创建用户（管理员功能）
     */
    @PostMapping("/users")
    public ApiResponse<User> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.createUser(request);
        // 不返回密码哈希
        user.setPasswordHash(null);
        return ApiResponse.success(user);
    }
    
    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 将 User 转为 AdminUserListItem，避免直接序列化实体导致闪退
     */
    private AdminUserListItem toListItem(User u) {
        AdminUserListItem item = new AdminUserListItem();
        item.setId(u.getId());
        item.setUsername(u.getUsername());
        item.setRealName(u.getRealName());
        item.setPasswordDisplay(u.getPasswordHash() != null && !u.getPasswordHash().isEmpty() ? "********" : "未设置");
        item.setRole(u.getRole() != null ? u.getRole().name() : "");
        item.setStatus(u.getStatus());
        item.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().format(ISO_DATETIME) : "");
        return item;
    }

    /**
     * 系统运行状态（内存、线程、DB连接等）
     */
    @GetMapping("/monitor/status")
    public ApiResponse<MonitorStatusResponse> getMonitorStatus() {
        MonitorStatusResponse status = new MonitorStatusResponse();

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMillis = runtimeMXBean.getUptime();
        status.setUptimeMillis(uptimeMillis);
        status.setUptime(formatUptime(uptimeMillis));

        Runtime runtime = Runtime.getRuntime();
        double usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0;
        double maxMb = runtime.maxMemory() / 1024.0 / 1024.0;
        status.setHeapUsedMb(Math.round(usedMb * 10) / 10.0);
        status.setHeapMaxMb(Math.round(maxMb * 10) / 10.0);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        status.setThreadCount(threadMXBean.getThreadCount());

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        status.setSystemLoadAverage(osBean.getSystemLoadAverage());

        status.setDbMetricsAvailable(false);
        status.setActiveDbConnections(0);
        status.setIdleDbConnections(0);
        status.setTotalDbConnections(0);
        status.setMaxDbConnections(0);

        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            try {
                status.setActiveDbConnections(hikari.getHikariPoolMXBean().getActiveConnections());
                status.setIdleDbConnections(hikari.getHikariPoolMXBean().getIdleConnections());
                status.setTotalDbConnections(hikari.getHikariPoolMXBean().getTotalConnections());
                status.setMaxDbConnections(hikari.getMaximumPoolSize());
                status.setDbMetricsAvailable(true);
            } catch (Exception ignored) {
                status.setDbMetricsAvailable(false);
            }
        }

        return ApiResponse.success(status);
    }

    /**
     * 最近的日志（按级别过滤，倒数 N 行）
     * level: ALL, DEBUG, INFO, WARN, ERROR, WARN_AND_ERROR（仅 WARN 与 ERROR）
     */
    @GetMapping("/monitor/logs")
    public ApiResponse<List<String>> getLogs(
            @RequestParam(name = "level", defaultValue = "ALL") String level,
            @RequestParam(name = "lines", defaultValue = "200") int lines) {
        Path path = Paths.get(logFilePath);
        if (!Files.exists(path)) {
            String msg = "日志文件不存在: " + path.toAbsolutePath();
            return ApiResponse.success(Collections.singletonList(msg));
        }
        try {
            List<String> allLines = Files.readAllLines(path);
            int fromIndex = Math.max(0, allLines.size() - lines);
            List<String> tail = new ArrayList<>(allLines.subList(fromIndex, allLines.size()));

            if (level != null && !level.isBlank() && !"ALL".equalsIgnoreCase(level)) {
                String key = level.toUpperCase();
                if ("WARN_AND_ERROR".equals(key)) {
                    tail = tail.stream()
                            .filter(s -> matchesLevel(s, "WARN") || matchesLevel(s, "ERROR"))
                            .collect(Collectors.toList());
                } else {
                    tail = tail.stream()
                            .filter(s -> matchesLevel(s, key))
                            .collect(Collectors.toList());
                }
            }

            return ApiResponse.success(tail);
        } catch (IOException e) {
            return ApiResponse.error("读取日志失败: " + e.getMessage());
        }
    }

    /** 判断一行日志是否包含指定级别（支持常见格式如 " ERROR "、"[ERROR]"、".ERROR " 等） */
    private static boolean matchesLevel(String line, String level) {
        if (line == null || level == null) return false;
        String u = level.toUpperCase();
        return line.contains(" " + u + " ") || line.contains("[" + u + "]")
                || line.contains("." + u + " ") || line.contains("." + u + "]")
                || line.contains("\t" + u + " ") || line.contains("\t" + u + "\t");
    }

    /**
     * 手动清除日志文件内容（清空文件，便于排查时只看新产生的日志）
     * 若日志正被写入（同进程占用），可能失败并返回提示。
     */
    @PostMapping("/monitor/logs/clear")
    public ApiResponse<Void> clearLogs() {
        Path path = Paths.get(logFilePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            return ApiResponse.success(null);
        }
        try {
            try (java.io.OutputStream os = Files.newOutputStream(path,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                // 清空文件
            }
            return ApiResponse.success(null);
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("being used") || msg.contains("另一个程序") || msg.contains("access") || msg.contains("拒绝")) {
                return ApiResponse.error("清除失败：日志文件正被占用，请稍后重试或重启后端服务后再清除");
            }
            return ApiResponse.error("清除日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取选题系统开放设置（全局开关 + 时间窗口）
     */
    @GetMapping("/selection-setting")
    public ApiResponse<SelectionSettingResponse> getSelectionSetting() {
        SystemSetting setting = systemSettingService.getOrCreate();
        SelectionSettingResponse resp = new SelectionSettingResponse();
        resp.setEnabled(setting.getSelectionEnabled() != null ? setting.getSelectionEnabled() : Boolean.TRUE);
        resp.setStartTime(setting.getSelectionStartTime());
        resp.setEndTime(setting.getSelectionEndTime());
        resp.setOpenNow(systemSettingService.isSelectionOpenNow());
        return ApiResponse.success(resp);
    }

    /**
     * 更新选题系统开放设置（全局开关 + 时间窗口）
     */
    @PostMapping("/selection-setting")
    public ApiResponse<SelectionSettingResponse> updateSelectionSetting(@RequestBody SelectionSettingRequest requestDto) {
        SystemSetting setting = systemSettingService.updateSelectionSetting(
                requestDto.getEnabled() != null ? requestDto.getEnabled() : Boolean.FALSE,
                requestDto.getStartTime(),
                requestDto.getEndTime()
        );
        SelectionSettingResponse resp = new SelectionSettingResponse();
        resp.setEnabled(setting.getSelectionEnabled());
        resp.setStartTime(setting.getSelectionStartTime());
        resp.setEndTime(setting.getSelectionEndTime());
        resp.setOpenNow(systemSettingService.isSelectionOpenNow());
        return ApiResponse.success(resp);
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分钟");
        sb.append(seconds).append("秒");
        return sb.toString();
    }

    /**
     * 获取所有教师账号列表
     */
    @GetMapping("/users/teachers")
    public ApiResponse<List<AdminUserListItem>> listTeachers() {
        List<User> list = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, Role.TEACHER).orderByDesc(User::getCreatedAt)
        );
        List<AdminUserListItem> result = new ArrayList<>();
        for (User u : list) {
            result.add(toListItem(u));
        }
        return ApiResponse.success(result);
    }

    /**
     * 获取所有学生账号列表
     */
    @GetMapping("/users/students")
    public ApiResponse<List<AdminUserListItem>> listStudents() {
        List<User> list = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, Role.STUDENT).orderByDesc(User::getCreatedAt)
        );
        List<AdminUserListItem> result = new ArrayList<>();
        for (User u : list) {
            result.add(toListItem(u));
        }
        return ApiResponse.success(result);
    }

    /**
     * 导师负荷概览：当前带学生数 / 最大可带 / 开放选题数 / 总选题数
     */
    @GetMapping("/teacher-load")
    public ApiResponse<List<TeacherLoadItemResponse>> getTeacherLoad() {
        List<User> teachers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, Role.TEACHER)
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId)
        );
        List<TeacherLoadItemResponse> result = new ArrayList<>();
        for (User t : teachers) {
            TeacherLoadItemResponse item = new TeacherLoadItemResponse();
            item.setTeacherId(t.getId());
            item.setRealName(t.getRealName());

            TeacherProfile profile = teacherProfileMapper.selectOne(
                    new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, t.getId())
            );
            item.setMaxStudents(profile != null ? profile.getMaxStudentCount() : 10);

            Long openTopics = topicMapper.selectCount(
                    new LambdaQueryWrapper<Topic>()
                            .eq(Topic::getTeacherId, t.getId())
                            .eq(Topic::getStatus, Topic.TopicStatus.OPEN)
            );
            Long totalTopics = topicMapper.selectCount(
                    new LambdaQueryWrapper<Topic>()
                            .eq(Topic::getTeacherId, t.getId())
            );
            item.setOpenTopics(openTopics != null ? openTopics.intValue() : 0);
            item.setTotalTopics(totalTopics != null ? totalTopics.intValue() : 0);

            Long students = topicApplicationMapper.countDistinctApprovedStudentsByTeacher(t.getId());
            item.setCurrentStudents(students != null ? students.intValue() : 0);

            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /**
     * 管理员修改指定用户密码（POST + body，与批量重置一致，避免 PUT 预检问题）
     */
    @PostMapping("/users/change-password")
    public ApiResponse<Void> updateUserPassword(@Valid @RequestBody AdminPasswordRequest request) {
        userService.updatePassword(request.getUserId(), request.getNewPassword());
        return ApiResponse.success(null);
    }

    /**
     * 管理员批量重置密码为默认密码 123456
     */
    @PostMapping("/users/reset-password")
    public ApiResponse<Integer> resetPasswordsToDefault(@Valid @RequestBody AdminResetPasswordRequest request) {
        int count = userService.resetPasswordsToDefault(request.getUserIds());
        return ApiResponse.success(count);
    }

    /**
     * 管理员批量删除账号（支持单个/多选）
     */
    @PostMapping("/users/delete")
    public ApiResponse<Integer> deleteUsers(@Valid @RequestBody AdminDeleteUsersRequest request, HttpServletRequest httpRequest) {
        Long adminId = getCurrentUserId(httpRequest);
        int count = userService.deleteUsers(request.getUserIds(), adminId);
        return ApiResponse.success(count);
    }
    
    /**
     * 获取待审核选题列表
     */
    @GetMapping("/topics/pending-review")
    public ApiResponse<List<TopicResponse>> getPendingReviewTopics() {
        List<Topic> topics = topicService.getTopics(Topic.TopicStatus.PENDING_REVIEW, null);
        List<TopicResponse> responses = convertToResponseList(topics);
        return ApiResponse.success(responses);
    }
    
    /**
     * 审核选题
     */
    @PostMapping("/topics/{id}/review")
    public ApiResponse<Void> reviewTopic(
            @PathVariable Long id,
            @RequestBody TopicReviewRequest requestDto,
            HttpServletRequest request) {
        Long adminId = getCurrentUserId(request);
        
        TopicReview.ReviewResult result;
        try {
            result = TopicReview.ReviewResult.valueOf(requestDto.getResult().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的审核结果");
        }
        
        topicService.reviewTopic(id, adminId, result, requestDto.getComment());
        return ApiResponse.success(null);
    }
    
    /**
     * 转换Topic实体为TopicResponse
     */
    private TopicResponse convertToResponse(Topic topic) {
        com.example.graduation.dto.TopicResponse response = new com.example.graduation.dto.TopicResponse();
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


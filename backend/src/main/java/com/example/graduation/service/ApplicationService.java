package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.entity.Thesis;
import com.example.graduation.entity.ThesisEvaluation;
import com.example.graduation.entity.CollabStage;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.ThesisMapper;
import com.example.graduation.mapper.ThesisEvaluationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationService {
    
    @Autowired
    private TopicApplicationMapper applicationMapper;
    
    @Autowired
    private TopicMapper topicMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private ThesisMapper thesisMapper;

    @Autowired
    private ThesisEvaluationMapper thesisEvaluationMapper;
    
    /**
     * 学生提交选题申请
     */
    @Transactional
    public TopicApplication submitApplication(Long topicId, Long studentId, String remark, BigDecimal matchScore) {
        // 全局选题开关与时间窗口控制
        if (!systemSettingService.isSelectionOpenNow()) {
            throw new RuntimeException("当前不在选题开放时间内，请联系管理员");
        }
        // 检查选题是否存在且开放
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("选题不存在");
        }
        
        if (topic.getStatus() != Topic.TopicStatus.OPEN) {
            throw new RuntimeException("选题未开放");
        }
        
        // 检查是否已申请
        TopicApplication existing = applicationMapper.selectOne(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getTopicId, topicId)
                        .eq(TopicApplication::getStudentId, studentId)
        );
        
        if (existing != null) {
            throw new RuntimeException("您已申请过此选题");
        }
        
        // 检查申请人数是否已满
        if (topic.getCurrentApplicants() >= topic.getMaxApplicants()) {
            throw new RuntimeException("该选题申请人数已满");
        }
        
        // 检查是否已有通过的申请
        TopicApplication approved = applicationMapper.selectOne(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStudentId, studentId)
                        .in(TopicApplication::getStatus,
                                TopicApplication.ApplicationStatus.APPROVED,
                                TopicApplication.ApplicationStatus.COMPLETION_PENDING,
                                TopicApplication.ApplicationStatus.COMPLETION_REJECTED,
                                TopicApplication.ApplicationStatus.COMPLETED)
        );
        
        if (approved != null) {
            throw new RuntimeException("您已有通过的选题申请，无法再次申请");
        }
        
        // 创建申请
        TopicApplication application = new TopicApplication();
        application.setTopicId(topicId);
        application.setStudentId(studentId);
        application.setStatus(TopicApplication.ApplicationStatus.PENDING);
        application.setRemark(remark);
        application.setMatchScore(matchScore);
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        
        applicationMapper.insert(application);
        
        // 更新选题当前申请人数
        topic.setCurrentApplicants(topic.getCurrentApplicants() + 1);
        topicMapper.updateById(topic);
        
        // 通知导师
        notificationService.createNotification(
                topic.getTeacherId(),
                "APPLICATION_SUBMIT",
                "新选题申请",
                "有学生申请您的选题《" + topic.getTitle() + "》",
                topicId
        );
        
        return application;
    }
    
    /**
     * 导师处理申请
     */
    @Transactional
    public void processApplication(Long applicationId, Long teacherId, 
                                   TopicApplication.ApplicationStatus status, String feedback) {
        TopicApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        
        // 验证导师权限
        Topic topic = topicMapper.selectById(application.getTopicId());
        if (!topic.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("无权限处理此申请");
        }
        
        if (application.getStatus() != TopicApplication.ApplicationStatus.PENDING) {
            throw new RuntimeException("申请已处理");
        }

        // 硬约束：同一选题最多只能有 1 条 APPROVED
        if (status == TopicApplication.ApplicationStatus.APPROVED) {
            TopicApplication alreadyApproved = applicationMapper.selectOne(
                    new LambdaQueryWrapper<TopicApplication>()
                            .eq(TopicApplication::getTopicId, application.getTopicId())
                            .in(TopicApplication::getStatus,
                                    TopicApplication.ApplicationStatus.APPROVED,
                                    TopicApplication.ApplicationStatus.COMPLETION_PENDING,
                                    TopicApplication.ApplicationStatus.COMPLETION_REJECTED,
                                    TopicApplication.ApplicationStatus.COMPLETED)
                            .last("LIMIT 1")
            );
            if (alreadyApproved != null) {
                throw new RuntimeException("该选题已被其他学生占用，无法再次通过申请");
            }
        }
        
        // 更新申请状态
        application.setStatus(status);
        application.setTeacherFeedback(feedback);
        application.setUpdatedAt(LocalDateTime.now());
        try {
            applicationMapper.updateById(application);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("uk_topic_approved") || msg.contains("Duplicate") || msg.contains("duplicate")) {
                throw new RuntimeException("该选题已被其他学生占用，无法再次通过申请");
            }
            throw e;
        }
        
        // 如果通过，关闭选题（如果申请人数已满）
        if (status == TopicApplication.ApplicationStatus.APPROVED) {
            if (topic.getCurrentApplicants() >= topic.getMaxApplicants()) {
                topic.setStatus(Topic.TopicStatus.CLOSED);
                topicMapper.updateById(topic);
            }
        } else {
            // 拒绝后，减少当前申请人数
            topic.setCurrentApplicants(Math.max(0, topic.getCurrentApplicants() - 1));
            topicMapper.updateById(topic);
        }
        
        // 通知学生
        String title = status == TopicApplication.ApplicationStatus.APPROVED ? "申请通过" : "申请未通过";
        notificationService.createNotification(
                application.getStudentId(),
                "APPLICATION_RESULT",
                title,
                "您的选题申请《" + topic.getTitle() + "》" + 
                        (status == TopicApplication.ApplicationStatus.APPROVED ? "已通过" : "未通过") +
                        (feedback != null ? "：" + feedback : ""),
                applicationId
        );
    }
    
    /**
     * 获取学生的申请列表
     */
    public List<TopicApplication> getStudentApplications(Long studentId) {
        return applicationMapper.selectList(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStudentId, studentId)
                        .orderByDesc(TopicApplication::getCreatedAt)
        );
    }
    
    /**
     * 获取选题的申请列表（按匹配度排序）
     */
    public List<TopicApplication> getTopicApplications(Long topicId, boolean sortByMatchScore) {
        LambdaQueryWrapper<TopicApplication> wrapper = new LambdaQueryWrapper<TopicApplication>()
                .eq(TopicApplication::getTopicId, topicId);
        
        if (sortByMatchScore) {
            wrapper.orderByDesc(TopicApplication::getMatchScore);
        } else {
            wrapper.orderByDesc(TopicApplication::getCreatedAt);
        }
        
        return applicationMapper.selectList(wrapper);
    }
    
    /**
     * 计算学生与选题的匹配度（对外仍暴露在 ApplicationService 上，内部委托给 MatchService）。
     *
     * 这样可以在不修改控制器调用方式的前提下，将匹配算法独立出来，后续升级只需改 MatchService。
     */
    public BigDecimal calculateMatchScore(Topic topic, Long studentId) {
        return matchService.calculateMatchScore(topic, studentId);
    }

    /**
     * 学生发起结题申请（仅已通过绑定可发起）。
     */
    @Transactional
    public void submitCompletionRequest(Long applicationId, Long studentId) {
        TopicApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!studentId.equals(app.getStudentId())) {
            throw new RuntimeException("无权操作该申请");
        }
        if (app.getStatus() != TopicApplication.ApplicationStatus.APPROVED
                && app.getStatus() != TopicApplication.ApplicationStatus.COMPLETION_REJECTED) {
            throw new RuntimeException("当前状态不可发起结题申请");
        }
        ensureCompletionReady(app.getTopicId(), app.getStudentId());
        app.setStatus(TopicApplication.ApplicationStatus.COMPLETION_PENDING);
        app.setUpdatedAt(LocalDateTime.now());
        applicationMapper.updateById(app);

        Topic topic = topicMapper.selectById(app.getTopicId());
        if (topic != null && topic.getTeacherId() != null) {
            notificationService.createNotification(
                    topic.getTeacherId(),
                    "COMPLETION_REQUEST",
                    "学生发起结题申请",
                    "学生已完成流程并提交结题申请，请审核。",
                    app.getId()
            );
        }
    }

    /**
     * 导师审批结题申请。
     */
    @Transactional
    public void reviewCompletionRequest(Long applicationId, Long teacherId, boolean approve, String feedback) {
        TopicApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new RuntimeException("申请不存在");
        }
        Topic topic = topicMapper.selectById(app.getTopicId());
        if (topic == null || !teacherId.equals(topic.getTeacherId())) {
            throw new RuntimeException("无权处理该结题申请");
        }
        if (app.getStatus() != TopicApplication.ApplicationStatus.COMPLETION_PENDING) {
            throw new RuntimeException("当前不在结题待审核状态");
        }
        app.setStatus(approve
                ? TopicApplication.ApplicationStatus.COMPLETED
                : TopicApplication.ApplicationStatus.COMPLETION_REJECTED);
        app.setTeacherFeedback(feedback);
        app.setUpdatedAt(LocalDateTime.now());
        applicationMapper.updateById(app);

        notificationService.createNotification(
                app.getStudentId(),
                "COMPLETION_RESULT",
                approve ? "结题申请已通过" : "结题申请未通过",
                approve ? "导师已通过你的结题申请。" : "导师未通过你的结题申请：" + (feedback == null ? "" : feedback),
                app.getId()
        );
    }

    private void ensureCompletionReady(Long topicId, Long studentId) {
        List<Thesis> all = thesisMapper.selectList(
                new LambdaQueryWrapper<Thesis>()
                        .eq(Thesis::getTopicId, topicId)
                        .eq(Thesis::getStudentId, studentId)
                        .orderByDesc(Thesis::getCreatedAt)
        );
        Map<String, Thesis> latestByStage = new HashMap<>();
        for (Thesis t : all) {
            if (t.getStage() == null || t.getStage().isBlank()) continue;
            latestByStage.putIfAbsent(t.getStage(), t);
        }
        for (CollabStage stage : CollabStage.ordered()) {
            Thesis latest = latestByStage.get(stage.name());
            if (latest == null || latest.getStatus() != Thesis.ThesisStatus.REVIEWED) {
                throw new RuntimeException("仍有环节未完成通过，暂不可申请结题");
            }
        }

        Thesis defense = latestByStage.get(CollabStage.THESIS_DEFENSE.name());
        if (defense == null) {
            throw new RuntimeException("缺少论文答辩环节记录，无法申请结题");
        }
        ThesisEvaluation eval = thesisEvaluationMapper.selectOne(
                new LambdaQueryWrapper<ThesisEvaluation>()
                        .eq(ThesisEvaluation::getThesisId, defense.getId())
                        .last("LIMIT 1")
        );
        if (eval == null || eval.getScore() == null || eval.getStudentScore() == null) {
            throw new RuntimeException("请先完成导师总评与学生评价后再申请结题");
        }
    }
}


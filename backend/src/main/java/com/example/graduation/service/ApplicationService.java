package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
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
                            .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
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
}


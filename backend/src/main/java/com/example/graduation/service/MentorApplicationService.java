package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.dto.MentorApplicationResponse;
import com.example.graduation.dto.TeacherOverviewResponse;
import com.example.graduation.dto.TopicResponse;
import com.example.graduation.entity.MentorApplication;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.entity.TopicMetrics;
import com.example.graduation.entity.User;
import com.example.graduation.entity.UserTag;
import com.example.graduation.entity.TeacherProfile;
import com.example.graduation.mapper.MentorApplicationMapper;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.TopicMetricsMapper;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.mapper.TeacherProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MentorApplicationService {

    @Autowired
    private MentorApplicationMapper mentorApplicationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private TopicApplicationMapper topicApplicationMapper;

    @Autowired
    private TopicMetricsMapper topicMetricsMapper;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private TagService tagService;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    /**
     * 学生创建拜师申请
     */
    @Transactional
    public MentorApplication createMentorApplication(Long studentId, Long teacherId, String reason) {
        // 硬约束：已结课（已完成毕设流程）的学生，不允许再次选导师/发起拜师
        Long completedCount = topicApplicationMapper.selectCount(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStudentId, studentId)
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.COMPLETED)
        );
        if (completedCount != null && completedCount > 0) {
            throw new RuntimeException("您已结课，无法再次申请导师或选题");
        }

        // 简单防重：同一学生对同一导师存在进行中的申请则不允许重复提交
        Long count = mentorApplicationMapper.selectCount(
                new LambdaQueryWrapper<MentorApplication>()
                        .eq(MentorApplication::getStudentId, studentId)
                        .eq(MentorApplication::getTeacherId, teacherId)
                        .in(MentorApplication::getStatus,
                                MentorApplication.Status.PENDING,
                                MentorApplication.Status.APPROVED)
        );
        if (count != null && count > 0) {
            throw new RuntimeException("已存在进行中的拜师申请或已通过的师生关系，请勿重复提交");
        }

        MentorApplication app = new MentorApplication();
        app.setStudentId(studentId);
        app.setTeacherId(teacherId);
        app.setStatus(MentorApplication.Status.PENDING);
        app.setReason(reason);
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        mentorApplicationMapper.insert(app);
        return app;
    }

    /**
     * 学生查看自己的拜师申请
     */
    public List<MentorApplication> getStudentApplications(Long studentId) {
        return mentorApplicationMapper.selectList(
                new LambdaQueryWrapper<MentorApplication>()
                        .eq(MentorApplication::getStudentId, studentId)
                        .orderByDesc(MentorApplication::getCreatedAt)
        );
    }

    /**
     * 导师查看收到的待处理拜师申请
     */
    public List<MentorApplication> getTeacherPendingApplications(Long teacherId) {
        return mentorApplicationMapper.selectList(
                new LambdaQueryWrapper<MentorApplication>()
                        .eq(MentorApplication::getTeacherId, teacherId)
                        .eq(MentorApplication::getStatus, MentorApplication.Status.PENDING)
                        .orderByDesc(MentorApplication::getCreatedAt)
        );
    }

    /**
     * 导师查看名下全部拜师申请（含待处理/已同意/已拒绝）
     */
    public List<MentorApplication> getTeacherApplications(Long teacherId) {
        return mentorApplicationMapper.selectList(
                new LambdaQueryWrapper<MentorApplication>()
                        .eq(MentorApplication::getTeacherId, teacherId)
                        .orderByDesc(MentorApplication::getCreatedAt)
        );
    }

    /**
     * 导师审批拜师申请
     */
    @Transactional
    public void handleMentorApplicationDecision(Long applicationId,
                                                Long teacherId,
                                                MentorApplication.Status decisionStatus,
                                                String comment) {
        MentorApplication app = mentorApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new RuntimeException("拜师申请不存在");
        }
        if (!teacherId.equals(app.getTeacherId())) {
            throw new RuntimeException("无权处理该拜师申请");
        }
        if (app.getStatus() != MentorApplication.Status.PENDING) {
            throw new RuntimeException("该拜师申请已处理");
        }

        if (decisionStatus != MentorApplication.Status.APPROVED
                && decisionStatus != MentorApplication.Status.REJECTED) {
            throw new RuntimeException("决策状态必须为 APPROVED 或 REJECTED");
        }

        app.setStatus(decisionStatus);
        app.setTeacherComment(comment);
        app.setUpdatedAt(LocalDateTime.now());
        mentorApplicationMapper.updateById(app);
    }

    /**
     * 为某条已通过的拜师申请指派课题，生成已通过的选题申请记录
     */
    @Transactional
    public TopicApplication assignTopicForApplication(Long applicationId,
                                                      Long teacherId,
                                                      Long topicId) {
        MentorApplication app = mentorApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new RuntimeException("拜师申请不存在");
        }
        if (!teacherId.equals(app.getTeacherId())) {
            throw new RuntimeException("无权为该申请指派课题");
        }
        if (app.getStatus() != MentorApplication.Status.APPROVED) {
            throw new RuntimeException("只有已通过的拜师申请才能指派课题");
        }

        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("选题不存在");
        }
        if (!teacherId.equals(topic.getTeacherId())) {
            throw new RuntimeException("只能指派本人名下的选题");
        }

        // 检查学生是否已有已通过的选题申请
        Long approvedCount = topicApplicationMapper.selectCount(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStudentId, app.getStudentId())
                        .in(TopicApplication::getStatus,
                                TopicApplication.ApplicationStatus.APPROVED,
                                TopicApplication.ApplicationStatus.COMPLETION_PENDING,
                                TopicApplication.ApplicationStatus.COMPLETION_REJECTED,
                                TopicApplication.ApplicationStatus.COMPLETED)
        );
        if (approvedCount != null && approvedCount > 0) {
            throw new RuntimeException("该学生已有已通过的选题申请，请先通过变更流程处理");
        }

        TopicApplication application = new TopicApplication();
        application.setTopicId(topicId);
        application.setStudentId(app.getStudentId());
        application.setStatus(TopicApplication.ApplicationStatus.APPROVED);
        application.setRemark(app.getReason());
        // 入口B复用入口A的“学生-题目”匹配算法，统一口径落库
        application.setMatchScore(applicationService.calculateMatchScore(topic, app.getStudentId()));
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        topicApplicationMapper.insert(application);

        // 更新选题当前申请人数
        Integer currentApplicants = topic.getCurrentApplicants() != null ? topic.getCurrentApplicants() : 0;
        topic.setCurrentApplicants(currentApplicants + 1);
        topic.setUpdatedAt(LocalDateTime.now());
        topicMapper.updateById(topic);

        return application;
    }

    /**
     * 为导师返回某条拜师申请可指派题目（仅本人、仅已通过申请、仅开放题目），并计算该学生匹配度。
     */
    public List<TopicResponse> getAssignableTopicsWithMatch(Long applicationId, Long teacherId) {
        MentorApplication app = mentorApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new RuntimeException("拜师申请不存在");
        }
        if (!teacherId.equals(app.getTeacherId())) {
            throw new RuntimeException("无权查看该申请的可指派题目");
        }
        if (app.getStatus() != MentorApplication.Status.APPROVED) {
            throw new RuntimeException("仅已通过的拜师申请可指派题目");
        }
        List<Topic> topics = topicMapper.selectList(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getTeacherId, teacherId)
                        .eq(Topic::getStatus, Topic.TopicStatus.OPEN)
                        .orderByDesc(Topic::getCreatedAt)
        );
        return topics.stream().map(topic -> {
            TopicResponse resp = new TopicResponse();
            resp.setId(topic.getId());
            resp.setTitle(topic.getTitle());
            resp.setStatus(topic.getStatus() != null ? topic.getStatus().name() : null);
            resp.setMatchScore(applicationService.calculateMatchScore(topic, app.getStudentId()));
            return resp;
        }).sorted((a, b) -> {
            if (a.getMatchScore() == null && b.getMatchScore() == null) return 0;
            if (a.getMatchScore() == null) return 1;
            if (b.getMatchScore() == null) return -1;
            return b.getMatchScore().compareTo(a.getMatchScore());
        }).toList();
    }

    /**
     * 学生端导师列表概览：可选传入 currentStudentId，用于计算“学生-导师”匹配度并按匹配度排序。
     */
    public List<TeacherOverviewResponse> getTeacherOverviewList(Long currentStudentId) {
        List<User> teachers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, User.Role.TEACHER)
        );
        if (teachers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> teacherIds = teachers.stream()
                .map(User::getId)
                .toList();

        // 导师档案
        List<TeacherProfile> profiles = teacherProfileMapper.selectList(
                new LambdaQueryWrapper<TeacherProfile>()
                        .in(TeacherProfile::getUserId, teacherIds)
        );
        Map<Long, TeacherProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(TeacherProfile::getUserId, p -> p));

        // 导师标签（逐个查询，数据量通常有限）
        Map<Long, List<UserTag>> tagsMap = new java.util.HashMap<>();
        for (Long teacherId : teacherIds) {
            List<UserTag> tags = tagService.getUserTags(teacherId);
            tagsMap.put(teacherId, tags);
        }

        // 选题信息
        List<Topic> topics = topicMapper.selectList(
                new LambdaQueryWrapper<Topic>()
                        .in(Topic::getTeacherId, teacherIds)
        );
        Map<Long, List<Topic>> topicsByTeacher = topics.stream()
                .collect(Collectors.groupingBy(Topic::getTeacherId));

        // 选题申请，用于计算当前学生数（按照 APPROVED 计）
        List<Long> topicIds = topics.stream()
                .map(Topic::getId)
                .toList();
        Map<Long, Integer> approvedCountByTopic = new java.util.HashMap<>();
        if (!topicIds.isEmpty()) {
            List<TopicApplication> applications = topicApplicationMapper.selectList(
                    new LambdaQueryWrapper<TopicApplication>()
                            .in(TopicApplication::getTopicId, topicIds)
                            .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
            );
            for (TopicApplication ta : applications) {
                Integer existing = approvedCountByTopic.get(ta.getTopicId());
                approvedCountByTopic.put(ta.getTopicId(), existing == null ? 1 : existing + 1);
            }
        }

        // 选题质量指标
        List<TopicMetrics> metricsList = topicMetricsMapper.selectList(
                new LambdaQueryWrapper<TopicMetrics>()
                        .in(TopicMetrics::getTeacherId, teacherIds)
        );
        Map<Long, List<TopicMetrics>> metricsByTeacher = metricsList.stream()
                .collect(Collectors.groupingBy(TopicMetrics::getTeacherId));

        // 当前学生标签（用于计算“学生-导师”匹配度）
        // - 只要传入 currentStudentId，就会返回 matchScore（即使学生尚未生成标签，也返回 0.50 作为中性值）
        List<UserTag> currentStudentTags = null;
        boolean shouldReturnMatchScore = currentStudentId != null;
        if (shouldReturnMatchScore) {
            currentStudentTags = tagService.getUserTags(currentStudentId);
        }

        List<TeacherOverviewResponse> result = new ArrayList<>();
        for (User teacher : teachers) {
            Long teacherId = teacher.getId();
            TeacherOverviewResponse resp = new TeacherOverviewResponse();
            resp.setTeacherId(teacherId);
            resp.setRealName(teacher.getRealName());

            TeacherProfile profile = profileMap.get(teacherId);
            if (profile != null) {
                resp.setTitle(profile.getTitle());
                resp.setResearchDirection(profile.getResearchDirection());
                resp.setMaxStudentCount(profile.getMaxStudentCount());
            }

            List<Topic> teacherTopics = topicsByTeacher.getOrDefault(teacherId, Collections.emptyList());
            int openCount = (int) teacherTopics.stream()
                    .filter(t -> t.getStatus() == Topic.TopicStatus.OPEN)
                    .count();
            resp.setOpenTopicCount(openCount);

            int currentStudents = 0;
            for (Topic t : teacherTopics) {
                Integer c = approvedCountByTopic.get(t.getId());
                if (c != null) {
                    currentStudents += c;
                }
            }
            resp.setCurrentStudentCount(currentStudents);

            // 标签：按权重排序取前若干个标签名
            List<UserTag> userTags = tagsMap.getOrDefault(teacherId, Collections.emptyList());
            List<String> tagNames = userTags.stream()
                    .sorted(Comparator.comparing(UserTag::getWeight).reversed())
                    .limit(5)
                    .map(UserTag::getTagName)
                    .toList();
            resp.setTags(tagNames);

            // 评价摘要：从 TopicMetrics 汇总
            List<TopicMetrics> teacherMetrics = metricsByTeacher.getOrDefault(teacherId, Collections.emptyList());
            if (!teacherMetrics.isEmpty()) {
                int totalStudents = 0;
                for (TopicMetrics m : teacherMetrics) {
                    Integer ts = m.getTotalStudents();
                    if (ts != null && ts > 0) {
                        totalStudents += ts;
                    }
                }
                resp.setTotalStudents(totalStudents);

                if (totalStudents > 0) {
                    BigDecimal sumScore = BigDecimal.ZERO;
                    BigDecimal sumExcellent = BigDecimal.ZERO;
                    BigDecimal sumFail = BigDecimal.ZERO;
                    for (TopicMetrics m : teacherMetrics) {
                        if (m.getAvgScore() != null && m.getTotalStudents() != null && m.getTotalStudents() > 0) {
                            sumScore = sumScore.add(
                                    m.getAvgScore().multiply(BigDecimal.valueOf(m.getTotalStudents()))
                            );
                        }
                        if (m.getExcellentRatio() != null && m.getTotalStudents() != null && m.getTotalStudents() > 0) {
                            sumExcellent = sumExcellent.add(
                                    m.getExcellentRatio().multiply(BigDecimal.valueOf(m.getTotalStudents()))
                            );
                        }
                        if (m.getFailRatio() != null && m.getTotalStudents() != null && m.getTotalStudents() > 0) {
                            sumFail = sumFail.add(
                                    m.getFailRatio().multiply(BigDecimal.valueOf(m.getTotalStudents()))
                            );
                        }
                    }
                    if (sumScore.compareTo(BigDecimal.ZERO) > 0) {
                        resp.setAvgScore(sumScore.divide(BigDecimal.valueOf(totalStudents), 2, java.math.RoundingMode.HALF_UP));
                    }
                    if (sumExcellent.compareTo(BigDecimal.ZERO) > 0) {
                        resp.setExcellentRatio(sumExcellent.divide(BigDecimal.valueOf(totalStudents), 4, java.math.RoundingMode.HALF_UP));
                    }
                    if (sumFail.compareTo(BigDecimal.ZERO) > 0) {
                        resp.setFailRatio(sumFail.divide(BigDecimal.valueOf(totalStudents), 4, java.math.RoundingMode.HALF_UP));
                    }
                }
            }

            // 计算“当前学生 ↔ 导师画像”的匹配度（0-1），学生端固定返回该字段
            if (shouldReturnMatchScore) {
                BigDecimal match = calculateStudentTeacherMatchScore(currentStudentTags, profile, userTags);
                resp.setMatchScore(match);
            }

            result.add(resp);
        }

        // 若存在匹配度，则按匹配度从高到低排序；否则保持原顺序
        if (shouldReturnMatchScore) {
            result.sort((a, b) -> {
                BigDecimal ma = a.getMatchScore();
                BigDecimal mb = b.getMatchScore();
                if (ma == null && mb == null) return 0;
                if (ma == null) return 1;
                if (mb == null) return -1;
                return mb.compareTo(ma);
            });
        }

        return result;
    }

    /**
     * 基于学生标签文本与“导师研究方向 + 导师标签”计算 0-1 匹配度。
     * 若向量服务不可用或文本缺失，返回 0.5 作为中性值。
     */
    private BigDecimal calculateStudentTeacherMatchScore(List<UserTag> studentTags,
                                                         TeacherProfile profile,
                                                         List<UserTag> teacherTags) {
        if (studentTags == null || studentTags.isEmpty()) {
            return new BigDecimal("0.50");
        }
        String studentText = studentTags.stream()
                .filter(t -> t != null && t.getTagName() != null)
                .map(UserTag::getTagName)
                .collect(Collectors.joining("，"));

        StringBuilder teacherSb = new StringBuilder();
        if (profile != null && profile.getResearchDirection() != null) {
            teacherSb.append(profile.getResearchDirection()).append(' ');
        }
        if (teacherTags != null && !teacherTags.isEmpty()) {
            String tagText = teacherTags.stream()
                    .filter(t -> t != null && t.getTagName() != null)
                    .map(UserTag::getTagName)
                    .collect(Collectors.joining("，"));
            teacherSb.append(tagText);
        }
        String teacherText = teacherSb.toString().trim();
        if (studentText.isEmpty() || teacherText.isEmpty()) {
            return new BigDecimal("0.50");
        }

        // 1) 优先使用向量模型（若已配置且服务可用）
        if (embeddingService != null) {
            float[] vS = embeddingService.embedText(studentText);
            float[] vT = embeddingService.embedText(teacherText);
            if (vS != null && vT != null && vS.length > 0 && vS.length == vT.length) {
                double dot = 0.0;
                double normS = 0.0;
                double normT = 0.0;
                for (int i = 0; i < vS.length; i++) {
                    float sv = vS[i];
                    float tv = vT[i];
                    dot += sv * tv;
                    normS += sv * sv;
                    normT += tv * tv;
                }
                if (dot != 0.0 && normS != 0.0 && normT != 0.0) {
                    double cosine = dot / (Math.sqrt(normS) * Math.sqrt(normT));
                    if (cosine < 0.0) cosine = 0.0;
                    if (cosine > 1.0) cosine = 1.0;
                    return new BigDecimal(cosine).setScale(4, java.math.RoundingMode.HALF_UP);
                }
            }
        }

        // 2) 回退：用标签权重重合度（不依赖外部服务），避免界面长期全是 0.50
        String teacherLower = teacherText.toLowerCase();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal matched = BigDecimal.ZERO;
        for (UserTag t : studentTags) {
            if (t == null || t.getTagName() == null || t.getWeight() == null) continue;
            String name = t.getTagName().trim().toLowerCase();
            if (name.isEmpty()) continue;
            BigDecimal w = t.getWeight();
            total = total.add(w);
            if (teacherLower.contains(name)) {
                matched = matched.add(w);
            }
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.50");
        }
        BigDecimal raw = matched.divide(total, 4, java.math.RoundingMode.HALF_UP);
        if (raw.compareTo(BigDecimal.ZERO) < 0) raw = BigDecimal.ZERO;
        if (raw.compareTo(BigDecimal.ONE) > 0) raw = BigDecimal.ONE;
        // 映射到 [0.30, 1.00]，与系统其它“匹配度”输出区间保持一致，避免出现大量 0%
        BigDecimal score = new BigDecimal("0.30").add(raw.multiply(new BigDecimal("0.70")));
        if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.ONE) > 0) score = BigDecimal.ONE;
        return score.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 将实体转成响应 DTO
     */
    public MentorApplicationResponse toResponse(MentorApplication app) {
        MentorApplicationResponse resp = new MentorApplicationResponse();
        resp.setId(app.getId());
        resp.setStudentId(app.getStudentId());
        resp.setTeacherId(app.getTeacherId());
        resp.setStatus(app.getStatus());
        resp.setReason(app.getReason());
        resp.setTeacherComment(app.getTeacherComment());
        resp.setCreatedAt(app.getCreatedAt());
        resp.setUpdatedAt(app.getUpdatedAt());

        // 透出姓名，便于前端列表直接展示，避免额外拉全量用户数据
        if (app.getStudentId() != null) {
            User student = userMapper.selectById(app.getStudentId());
            if (student != null) {
                resp.setStudentName(student.getRealName());
            }
        }
        if (app.getTeacherId() != null) {
            User teacher = userMapper.selectById(app.getTeacherId());
            if (teacher != null) {
                resp.setTeacherName(teacher.getRealName());
            }
        }
        return resp;
    }
}


package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.dto.CollabStageProgressItemResponse;
import com.example.graduation.dto.CollabStageWindowItemRequest;
import com.example.graduation.entity.*;
import com.example.graduation.mapper.CollabStageWindowMapper;
import com.example.graduation.mapper.ThesisMapper;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollabService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private TopicApplicationMapper applicationMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private CollabStageWindowMapper stageWindowMapper;

    @Autowired
    private ThesisMapper thesisMapper;

    @Autowired
    private SystemSettingService systemSettingService;

    public void assertApplicationParticipant(Long userId, String role, TopicApplication app) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if ("STUDENT".equalsIgnoreCase(role) && app.getStudentId() != null && app.getStudentId().equals(userId)) {
            return;
        }
        if ("TEACHER".equalsIgnoreCase(role) && app.getTopicId() != null) {
            Topic topic = topicMapper.selectById(app.getTopicId());
            if (topic != null && topic.getTeacherId() != null && topic.getTeacherId().equals(userId)) {
                return;
            }
        }
        throw new RuntimeException("无权查看该协作申请");
    }

    /**
     * 校验导师设置的时间窗落在管理员配置的毕业季总窗口内（若管理员未配置则不限制）。
     */
    public void validateStageWindowsAgainstSeason(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return;
        }
        if (start == null || end == null) {
            throw new RuntimeException("环节开放时间需同时设置开始与结束，或均留空表示未开放");
        }
        if (!start.isBefore(end)) {
            throw new RuntimeException("环节开始时间必须早于结束时间");
        }
        SystemSetting s = systemSettingService.getOrCreate();
        LocalDateTime gs = s.getGraduationSeasonStart();
        LocalDateTime ge = s.getGraduationSeasonEnd();
        if (gs != null && start.isBefore(gs)) {
            throw new RuntimeException("环节开始时间不能早于管理员设置的毕业季开始时间");
        }
        if (ge != null && end.isAfter(ge)) {
            throw new RuntimeException("环节结束时间不能晚于管理员设置的毕业季结束时间");
        }
        if (gs != null && ge != null && (start.isAfter(ge) || end.isBefore(gs))) {
            throw new RuntimeException("环节时间必须在管理员设置的毕业季时间范围内");
        }
    }

    public List<CollabStageProgressItemResponse> buildProgress(Long applicationId, Long userId, String role) {
        TopicApplication app = applicationMapper.selectById(applicationId);
        if (app == null || (app.getStatus() != TopicApplication.ApplicationStatus.APPROVED
                && app.getStatus() != TopicApplication.ApplicationStatus.COMPLETION_PENDING
                && app.getStatus() != TopicApplication.ApplicationStatus.COMPLETION_REJECTED
                && app.getStatus() != TopicApplication.ApplicationStatus.COMPLETED)) {
            throw new RuntimeException("申请不存在或状态不可用");
        }
        assertApplicationParticipant(userId, role, app);

        List<CollabStageWindow> windows = stageWindowMapper.selectList(
                new LambdaQueryWrapper<CollabStageWindow>()
                        .eq(CollabStageWindow::getApplicationId, applicationId)
        );
        Map<String, CollabStageWindow> winByStage = windows.stream()
                .collect(Collectors.toMap(CollabStageWindow::getStage, w -> w, (a, b) -> b));

        List<Thesis> allTheses = thesisMapper.selectList(
                new LambdaQueryWrapper<Thesis>()
                        .eq(Thesis::getTopicId, app.getTopicId())
                        .eq(Thesis::getStudentId, app.getStudentId())
                        .orderByDesc(Thesis::getCreatedAt)
        );
        Map<String, Thesis> latestByStage = new HashMap<>();
        for (Thesis t : allTheses) {
            if (t.getStage() == null || t.getStage().isBlank()) {
                continue;
            }
            latestByStage.putIfAbsent(t.getStage(), t);
        }

        LocalDateTime now = LocalDateTime.now();
        List<CollabStageProgressItemResponse> out = new ArrayList<>();
        int idx = 1;

        // 三阶段：阶段内并行；阶段之间强制先后（上一阶段全通过，下一阶段才解锁）
        Map<CollabStage.Phase, Boolean> phaseComplete = calcPhaseComplete(latestByStage);
        for (CollabStage st : CollabStage.ordered()) {
            CollabStageProgressItemResponse row = new CollabStageProgressItemResponse();
            row.setOrderIndex(idx++);
            row.setPhaseIndex(st.getPhase().getIndex());
            row.setPhaseLabel(st.getPhase().getLabel());
            row.setStage(st.name());
            row.setStageLabel(st.getLabel());
            CollabStageWindow w = winByStage.get(st.name());
            if (w != null) {
                row.setWindowStart(w.getWindowStart());
                row.setWindowEnd(w.getWindowEnd());
            }
            String access = resolveAccessState(now, w);
            row.setAccessState(access);
            row.setTimePlanText(buildTimePlanText(w, access));

            Thesis latest = latestByStage.get(st.name());
            // 先计算基础提交状态
            String baseSubmission = resolveSubmissionStatus(access, latest);
            // 阶段锁：第二阶段需第一阶段全通过；第三阶段需第二阶段全通过
            if (st.getPhase() == CollabStage.Phase.BEFORE_DEFENSE && !phaseComplete.get(CollabStage.Phase.BEFORE_MIDTERM)) {
                baseSubmission = "BLOCKED_BY_PREVIOUS";
            }
            if (st.getPhase() == CollabStage.Phase.AFTER_DEFENSE && !phaseComplete.get(CollabStage.Phase.BEFORE_DEFENSE)) {
                baseSubmission = "BLOCKED_BY_PREVIOUS";
            }
            row.setSubmissionStatus(baseSubmission);
            row.setSubmissionStatusLabel(labelSubmission(row.getSubmissionStatus()));
            if (latest != null) {
                row.setLatestThesisId(latest.getId());
                row.setLatestFileName(latest.getFileName());
                row.setLatestUpdatedAt(latest.getUpdatedAt());
            }
            out.add(row);
        }
        return out;
    }

    private static Map<CollabStage.Phase, Boolean> calcPhaseComplete(Map<String, Thesis> latestByStage) {
        Map<CollabStage.Phase, Boolean> ok = new EnumMap<>(CollabStage.Phase.class);
        for (CollabStage.Phase p : CollabStage.Phase.values()) {
            ok.put(p, Boolean.TRUE);
        }
        for (CollabStage st : CollabStage.ordered()) {
            Thesis latest = latestByStage.get(st.name());
            boolean approved = latest != null && latest.getStatus() == Thesis.ThesisStatus.REVIEWED;
            if (!approved) {
                ok.put(st.getPhase(), Boolean.FALSE);
            }
        }
        return ok;
    }

    private static String buildTimePlanText(CollabStageWindow w, String access) {
        if (w == null || w.getWindowStart() == null || w.getWindowEnd() == null) {
            return "未开放（导师未设置时间）";
        }
        return w.getWindowStart().format(FMT) + " — " + w.getWindowEnd().format(FMT);
    }

    private static String resolveAccessState(LocalDateTime now, CollabStageWindow w) {
        if (w == null || w.getWindowStart() == null || w.getWindowEnd() == null) {
            return "NOT_CONFIGURED";
        }
        if (now.isBefore(w.getWindowStart())) {
            return "NOT_OPEN_YET";
        }
        if (now.isAfter(w.getWindowEnd())) {
            return "ENDED";
        }
        return "OPEN";
    }

    private static String resolveSubmissionStatus(String access, Thesis latest) {
        // 对导师审核来说，只要已有稿件，就应按稿件状态展示，不应被时间窗覆盖成 NONE。
        if (latest != null) {
            Thesis.ThesisStatus s = latest.getStatus();
            if (s == Thesis.ThesisStatus.NEED_REVISION) {
                return "NEED_REVISION";
            }
            if (s == Thesis.ThesisStatus.REVIEWED) {
                return "APPROVED";
            }
            return "UNDER_REVIEW";
        }
        if (!"OPEN".equals(access)) {
            return "NONE";
        }
        return "PENDING_SUBMIT";
    }

    private static String labelSubmission(String code) {
        if (code == null) {
            return "";
        }
        if ("NONE".equals(code)) {
            return "—";
        }
        if ("PENDING_SUBMIT".equals(code)) {
            return "待提交";
        }
        if ("UNDER_REVIEW".equals(code)) {
            return "审核中";
        }
        if ("NEED_REVISION".equals(code)) {
            return "退回修改";
        }
        if ("APPROVED".equals(code)) {
            return "已通过";
        }
        if ("BLOCKED_BY_PREVIOUS".equals(code)) {
            return "待前一环节通过";
        }
        return code;
    }

    /**
     * 导师（或管理员）批量保存各环节时间窗。
     */
    @Transactional
    public void saveStageWindows(Long applicationId, Long operatorUserId, String role,
                                 List<CollabStageWindowItemRequest> items) {
        TopicApplication app = applicationMapper.selectById(applicationId);
        if (app == null || app.getStatus() != TopicApplication.ApplicationStatus.APPROVED) {
            throw new RuntimeException("申请不存在或未通过");
        }
        if (!"ADMIN".equalsIgnoreCase(role)) {
            Topic topic = topicMapper.selectById(app.getTopicId());
            if (topic == null || topic.getTeacherId() == null || !topic.getTeacherId().equals(operatorUserId)) {
                throw new RuntimeException("仅导师可为该学生设置环节时间");
            }
        }

        Set<String> allowed = Arrays.stream(CollabStage.values()).map(Enum::name).collect(Collectors.toSet());
        for (CollabStageWindowItemRequest it : items) {
            if (it.getStage() == null || !allowed.contains(it.getStage())) {
                throw new RuntimeException("无效环节: " + it.getStage());
            }
            validateStageWindowsAgainstSeason(it.getWindowStart(), it.getWindowEnd());

            CollabStageWindow existing = stageWindowMapper.selectOne(
                    new LambdaQueryWrapper<CollabStageWindow>()
                            .eq(CollabStageWindow::getApplicationId, applicationId)
                            .eq(CollabStageWindow::getStage, it.getStage())
            );
            if (existing == null) {
                existing = new CollabStageWindow();
                existing.setApplicationId(applicationId);
                existing.setStage(it.getStage());
            }
            existing.setWindowStart(it.getWindowStart());
            existing.setWindowEnd(it.getWindowEnd());
            existing.setUpdatedAt(LocalDateTime.now());
            if (existing.getId() == null) {
                stageWindowMapper.insert(existing);
            } else {
                stageWindowMapper.updateById(existing);
            }
        }
    }

    /** 学生上传前：环节必须处于开放窗口，且无「审核中」的待处理稿件；阶段之间强制顺序，阶段内可并行 */
    public void assertStudentCanUpload(Long studentId, Long topicId, CollabStage stage) {
        TopicApplication app = applicationMapper.selectOne(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getTopicId, topicId)
                        .eq(TopicApplication::getStudentId, studentId)
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
        );
        if (app == null) {
            throw new RuntimeException("您尚未获得该选题，无法提交");
        }
        CollabStageWindow w = stageWindowMapper.selectOne(
                new LambdaQueryWrapper<CollabStageWindow>()
                        .eq(CollabStageWindow::getApplicationId, app.getId())
                        .eq(CollabStageWindow::getStage, stage.name())
        );
        String access = resolveAccessState(LocalDateTime.now(), w);
        if (!"OPEN".equals(access)) {
            throw new RuntimeException("当前环节未开放，请等待导师设置时间或到达开放窗口");
        }
        // 阶段强制先后：第二阶段需第一阶段全通过；第三阶段需第二阶段全通过
        if (stage.getPhase() == CollabStage.Phase.BEFORE_DEFENSE) {
            assertPhaseComplete(topicId, studentId, CollabStage.Phase.BEFORE_MIDTERM);
        }
        if (stage.getPhase() == CollabStage.Phase.AFTER_DEFENSE) {
            assertPhaseComplete(topicId, studentId, CollabStage.Phase.BEFORE_DEFENSE);
        }
        Thesis latest = thesisMapper.selectOne(
                new LambdaQueryWrapper<Thesis>()
                        .eq(Thesis::getTopicId, topicId)
                        .eq(Thesis::getStudentId, studentId)
                        .eq(Thesis::getStage, stage.name())
                        .orderByDesc(Thesis::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (latest != null && latest.getStatus() == Thesis.ThesisStatus.UPLOADED) {
            throw new RuntimeException("当前环节尚有审核中的稿件，请等待导师处理后再提交");
        }
    }

    private void assertPhaseComplete(Long topicId, Long studentId, CollabStage.Phase phase) {
        for (CollabStage st : CollabStage.ordered()) {
            if (st.getPhase() != phase) {
                continue;
            }
            Thesis latest = thesisMapper.selectOne(
                    new LambdaQueryWrapper<Thesis>()
                            .eq(Thesis::getTopicId, topicId)
                            .eq(Thesis::getStudentId, studentId)
                            .eq(Thesis::getStage, st.name())
                            .orderByDesc(Thesis::getCreatedAt)
                            .last("LIMIT 1")
            );
            if (latest == null || latest.getStatus() != Thesis.ThesisStatus.REVIEWED) {
                throw new RuntimeException("请先完成并通过上一阶段的环节【" + st.getLabel() + "】后再进入下一阶段");
            }
        }
    }
}

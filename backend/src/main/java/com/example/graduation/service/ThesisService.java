package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.CollabStage;
import com.example.graduation.entity.Thesis;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.mapper.ThesisMapper;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ThesisService {

    public static final String LEGACY_STAGE_CODE = "LEGACY_FILE";

    @Autowired
    private ThesisMapper thesisMapper;

    @Autowired
    private TopicApplicationMapper applicationMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CollabService collabService;

    /**
     * 上传环节稿件：按环节独立成记录，同一环节可有多条版本（退回后再传）。
     */
    @Transactional
    public Thesis uploadThesis(Long topicId, Long studentId, String fileUrl, String fileName, Long fileSize, CollabStage stage) {
        Objects.requireNonNull(stage, "环节不能为空");
        TopicApplication application = applicationMapper.selectOne(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getTopicId, topicId)
                        .eq(TopicApplication::getStudentId, studentId)
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
        );

        if (application == null) {
            throw new RuntimeException("您尚未获得该选题，无法上传论文");
        }

        collabService.assertStudentCanUpload(studentId, topicId, stage);

        Thesis thesis = new Thesis();
        thesis.setTopicId(topicId);
        thesis.setStudentId(studentId);
        thesis.setFileUrl(fileUrl);
        thesis.setFileName(fileName);
        thesis.setFileSize(fileSize);
        thesis.setStage(stage.name());
        thesis.setStatus(Thesis.ThesisStatus.UPLOADED);
        thesis.setCreatedAt(LocalDateTime.now());
        thesis.setUpdatedAt(LocalDateTime.now());
        thesisMapper.insert(thesis);

        Topic topic = topicMapper.selectById(topicId);
        if (topic != null && topic.getTeacherId() != null) {
            notificationService.createNotification(
                    topic.getTeacherId(),
                    "THESIS_UPLOADED",
                    "环节稿件已上传",
                    "学生已上传《" + fileName + "》（" + stage.getLabel() + "），请及时审核",
                    thesis.getId()
            );
        }

        return thesis;
    }

    /**
     * 导师或管理员：审核环节稿件（通过与格子达「审核中」衔接；打分录入仍走评价接口）。
     */
    @Transactional
    public void reviewThesisWorkflow(Long thesisId, Long operatorId, String role, boolean approve) {
        Thesis thesis = thesisMapper.selectById(thesisId);
        if (thesis == null) {
            throw new RuntimeException("记录不存在");
        }
        Topic topic = topicMapper.selectById(thesis.getTopicId());
        if (topic == null) {
            throw new RuntimeException("选题不存在");
        }
        boolean admin = "ADMIN".equalsIgnoreCase(role);
        if (!admin && (topic.getTeacherId() == null || !topic.getTeacherId().equals(operatorId))) {
            throw new RuntimeException("仅导师或管理员可审核该稿件");
        }
        if (thesis.getStatus() != Thesis.ThesisStatus.UPLOADED) {
            throw new RuntimeException("当前状态不可执行该审核操作");
        }
        thesis.setStatus(approve ? Thesis.ThesisStatus.REVIEWED : Thesis.ThesisStatus.NEED_REVISION);
        thesis.setUpdatedAt(LocalDateTime.now());
        thesisMapper.updateById(thesis);

        String stageLabel = thesis.getStage();
        try {
            stageLabel = CollabStage.fromCode(thesis.getStage()).getLabel();
        } catch (Exception ignored) {
        }
        String title = approve ? "环节稿件已通过" : "环节稿件需修改";
        String content = approve
                ? "您的「" + stageLabel + "」稿件已通过审核。"
                : "您的「" + stageLabel + "」稿件需修改，请查看导师意见后重新提交。";
        notificationService.createNotification(thesis.getStudentId(), "THESIS_WORKFLOW", title, content, thesis.getId());
    }

    public List<Thesis> getStudentTheses(Long studentId) {
        return thesisMapper.selectList(
                new LambdaQueryWrapper<Thesis>()
                        .eq(Thesis::getStudentId, studentId)
                        .orderByDesc(Thesis::getCreatedAt)
        );
    }

    public List<Thesis> getTeacherTheses(Long teacherId) {
        List<Topic> topics = topicMapper.selectList(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getTeacherId, teacherId)
        );
        if (topics.isEmpty()) {
            return List.of();
        }
        Set<Long> topicIds = topics.stream().map(Topic::getId).collect(Collectors.toSet());
        return thesisMapper.selectList(
                new LambdaQueryWrapper<Thesis>()
                        .in(Thesis::getTopicId, topicIds)
                        .orderByDesc(Thesis::getCreatedAt)
        );
    }

    public Thesis getThesis(Long thesisId) {
        return thesisMapper.selectById(thesisId);
    }
}

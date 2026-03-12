package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.ChangeRequest;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicApplication;
import com.example.graduation.mapper.ChangeRequestMapper;
import com.example.graduation.mapper.TopicApplicationMapper;
import com.example.graduation.mapper.TopicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChangeRequestService {

    @Autowired
    private ChangeRequestMapper changeRequestMapper;

    @Autowired
    private TopicApplicationMapper topicApplicationMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * 创建新的变更申请（换题 / 换导师）
     */
    @Transactional
    public ChangeRequest createChangeRequest(Long studentId,
                                             ChangeRequest.ChangeType type,
                                             String reason,
                                             Long targetTopicId,
                                             Long targetTeacherId) {
        // 找到当前生效的选题申请（唯一 APPROVED）
        TopicApplication approved = topicApplicationMapper.selectOne(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getStudentId, studentId)
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
        );
        if (approved == null) {
            throw new RuntimeException("当前不存在已通过的选题申请，无法发起变更");
        }

        // 检查是否已有未完成的变更申请
        Long count = changeRequestMapper.selectCount(
                new LambdaQueryWrapper<ChangeRequest>()
                        .eq(ChangeRequest::getStudentId, studentId)
                        .eq(ChangeRequest::getCurrentApplicationId, approved.getId())
                        .in(ChangeRequest::getStatus,
                                ChangeRequest.ChangeStatus.PENDING_TEACHER,
                                ChangeRequest.ChangeStatus.PENDING_ADMIN)
        );
        if (count != null && count > 0) {
            throw new RuntimeException("已有进行中的变更申请，请等待处理结果");
        }

        ChangeRequest request = new ChangeRequest();
        request.setStudentId(studentId);
        request.setCurrentApplicationId(approved.getId());
        request.setType(type);
        request.setReason(reason);
        request.setTargetTopicId(targetTopicId);
        request.setTargetTeacherId(targetTeacherId);
        // 换题默认先由导师处理，换导师默认直接进入管理员待处理队列
        request.setStatus(type == ChangeRequest.ChangeType.CHANGE_TOPIC
                ? ChangeRequest.ChangeStatus.PENDING_TEACHER
                : ChangeRequest.ChangeStatus.PENDING_ADMIN);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        changeRequestMapper.insert(request);
        return request;
    }

    /**
     * 查询某学生的所有变更申请
     */
    public List<ChangeRequest> getStudentRequests(Long studentId) {
        return changeRequestMapper.selectList(
                new LambdaQueryWrapper<ChangeRequest>()
                        .eq(ChangeRequest::getStudentId, studentId)
                        .orderByDesc(ChangeRequest::getCreatedAt)
        );
    }

    /**
     * 导师查看名下待处理的“更换选题”申请
     */
    public List<ChangeRequest> getTeacherPendingRequests(Long teacherId) {
        // 先取出所有待导师处理的变更申请
        List<ChangeRequest> pending = changeRequestMapper.selectList(
                new LambdaQueryWrapper<ChangeRequest>()
                        .eq(ChangeRequest::getStatus, ChangeRequest.ChangeStatus.PENDING_TEACHER)
        );
        // 在内存中过滤出当前导师名下的（通过 application -> topic.teacherId 判断）
        return pending.stream()
                .filter(req -> {
                    TopicApplication app = topicApplicationMapper.selectById(req.getCurrentApplicationId());
                    if (app == null) return false;
                    Topic topic = topicMapper.selectById(app.getTopicId());
                    return topic != null && teacherId.equals(topic.getTeacherId());
                })
                .toList();
    }

    /**
     * 管理员查看待处理的“更换导师”申请
     */
    public List<ChangeRequest> getAdminPendingRequests() {
        return changeRequestMapper.selectList(
                new LambdaQueryWrapper<ChangeRequest>()
                        .eq(ChangeRequest::getType, ChangeRequest.ChangeType.CHANGE_TEACHER)
                        .eq(ChangeRequest::getStatus, ChangeRequest.ChangeStatus.PENDING_ADMIN)
                        .orderByDesc(ChangeRequest::getCreatedAt)
        );
    }

    /**
     * 导师审批“更换选题”申请（当前版本：同意=解除绑定，拒绝=维持原绑定）
     */
    @Transactional
    public void handleTeacherDecision(Long requestId, Long teacherId,
                                      ChangeRequest.Decision decision,
                                      String comment) {
        ChangeRequest request = changeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new RuntimeException("变更申请不存在");
        }
        if (request.getType() != ChangeRequest.ChangeType.CHANGE_TOPIC) {
            throw new RuntimeException("该变更申请不属于更换选题类型");
        }
        if (request.getStatus() != ChangeRequest.ChangeStatus.PENDING_TEACHER) {
            throw new RuntimeException("该变更申请已处理");
        }

        TopicApplication app = topicApplicationMapper.selectById(request.getCurrentApplicationId());
        if (app == null) {
            throw new RuntimeException("关联的选题申请不存在");
        }
        Topic topic = topicMapper.selectById(app.getTopicId());
        if (topic == null || !teacherId.equals(topic.getTeacherId())) {
            throw new RuntimeException("无权限处理该变更申请");
        }

        request.setTeacherDecision(decision);
        request.setTeacherComment(comment);
        request.setUpdatedAt(LocalDateTime.now());

        if (decision == ChangeRequest.Decision.APPROVED) {
            // 同意更换选题：解除当前绑定
            app.setStatus(TopicApplication.ApplicationStatus.REJECTED);
            app.setUpdatedAt(LocalDateTime.now());
            topicApplicationMapper.updateById(app);

            topic.setCurrentApplicants(Math.max(0, topic.getCurrentApplicants() - 1));
            topicMapper.updateById(topic);

            request.setStatus(ChangeRequest.ChangeStatus.APPROVED);

            // 通知学生：导师已同意解除绑定，可以重新选题
            notificationService.createNotification(
                    request.getStudentId(),
                    "CHANGE_TOPIC_APPROVED",
                    "选题变更已通过",
                    "导师已同意解除当前选题《" + topic.getTitle() + "》的绑定，您可以在选题中心重新选择题目。"
                            + (comment != null ? " 导师备注：" + comment : ""),
                    request.getId()
            );
        } else {
            // 拒绝更换选题
            request.setStatus(ChangeRequest.ChangeStatus.REJECTED);

            notificationService.createNotification(
                    request.getStudentId(),
                    "CHANGE_TOPIC_REJECTED",
                    "选题变更未通过",
                    "导师未同意更换当前选题《" + topic.getTitle() + "》。"
                            + (comment != null ? " 导师说明：" + comment : ""),
                    request.getId()
            );
        }

        changeRequestMapper.updateById(request);
    }

    /**
     * 管理员审批“更换导师”申请（当前版本：同意=解除绑定，学生重新选题）
     */
    @Transactional
    public void handleAdminDecision(Long requestId,
                                    ChangeRequest.Decision decision,
                                    String comment) {
        ChangeRequest request = changeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new RuntimeException("变更申请不存在");
        }
        if (request.getType() != ChangeRequest.ChangeType.CHANGE_TEACHER) {
            throw new RuntimeException("该变更申请不属于更换导师类型");
        }
        if (request.getStatus() != ChangeRequest.ChangeStatus.PENDING_ADMIN) {
            throw new RuntimeException("该变更申请已处理");
        }

        TopicApplication app = topicApplicationMapper.selectById(request.getCurrentApplicationId());
        if (app == null) {
            throw new RuntimeException("关联的选题申请不存在");
        }
        Topic topic = topicMapper.selectById(app.getTopicId());
        if (topic == null) {
            throw new RuntimeException("关联的选题不存在");
        }

        request.setAdminDecision(decision);
        request.setAdminComment(comment);
        request.setUpdatedAt(LocalDateTime.now());

        if (decision == ChangeRequest.Decision.APPROVED) {
            // 同意更换导师：当前版本仅解除绑定，学生重新选题/申请
            app.setStatus(TopicApplication.ApplicationStatus.REJECTED);
            app.setUpdatedAt(LocalDateTime.now());
            topicApplicationMapper.updateById(app);

            topic.setCurrentApplicants(Math.max(0, topic.getCurrentApplicants() - 1));
            topicMapper.updateById(topic);

            request.setStatus(ChangeRequest.ChangeStatus.APPROVED);

            // 通知学生和原导师
            notificationService.createNotification(
                    request.getStudentId(),
                    "CHANGE_TEACHER_APPROVED",
                    "更换导师申请已通过",
                    "管理员已同意您更换当前导师《" + topic.getTitle() + "》对应的指导关系，您可以重新选择导师与题目。"
                            + (comment != null ? " 管理员说明：" + comment : ""),
                    request.getId()
            );
            notificationService.createNotification(
                    topic.getTeacherId(),
                    "CHANGE_TEACHER_APPROVED",
                    "学生已更换导师",
                    "管理员已同意学生更换论文导师，原选题《" + topic.getTitle() + "》与该学生的绑定已解除。",
                    request.getId()
            );
        } else {
            // 拒绝更换导师
            request.setStatus(ChangeRequest.ChangeStatus.REJECTED);

            notificationService.createNotification(
                    request.getStudentId(),
                    "CHANGE_TEACHER_REJECTED",
                    "更换导师申请未通过",
                    "管理员未同意更换当前导师。"
                            + (comment != null ? " 管理员说明：" + comment : ""),
                    request.getId()
            );
        }

        changeRequestMapper.updateById(request);
    }
}


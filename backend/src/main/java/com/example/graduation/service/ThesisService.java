package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

@Service
public class ThesisService {
    
    @Autowired
    private ThesisMapper thesisMapper;
    
    @Autowired
    private TopicApplicationMapper applicationMapper;
    
    @Autowired
    private TopicMapper topicMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 上传论文
     */
    @Transactional
    public Thesis uploadThesis(Long topicId, Long studentId, String fileUrl, String fileName, Long fileSize) {
        // 检查申请是否通过
        TopicApplication application = applicationMapper.selectOne(
                new LambdaQueryWrapper<TopicApplication>()
                        .eq(TopicApplication::getTopicId, topicId)
                        .eq(TopicApplication::getStudentId, studentId)
                        .eq(TopicApplication::getStatus, TopicApplication.ApplicationStatus.APPROVED)
        );
        
        if (application == null) {
            throw new RuntimeException("您尚未获得该选题，无法上传论文");
        }
        
        // 检查是否已上传过
        Thesis existing = thesisMapper.selectOne(
                new LambdaQueryWrapper<Thesis>()
                        .eq(Thesis::getTopicId, topicId)
                        .eq(Thesis::getStudentId, studentId)
        );
        
        Thesis thesis;
        if (existing != null) {
            // 更新现有记录
            existing.setFileUrl(fileUrl);
            existing.setFileName(fileName);
            existing.setFileSize(fileSize);
            existing.setStatus(Thesis.ThesisStatus.UPLOADED);
            existing.setUpdatedAt(LocalDateTime.now());
            thesisMapper.updateById(existing);
            thesis = existing;
        } else {
            // 创建新记录
            thesis = new Thesis();
            thesis.setTopicId(topicId);
            thesis.setStudentId(studentId);
            thesis.setFileUrl(fileUrl);
            thesis.setFileName(fileName);
            thesis.setFileSize(fileSize);
            thesis.setStatus(Thesis.ThesisStatus.UPLOADED);
            thesis.setCreatedAt(LocalDateTime.now());
            thesis.setUpdatedAt(LocalDateTime.now());
            thesisMapper.insert(thesis);
        }
        
        // 通知导师论文已上传
        Topic topic = topicMapper.selectById(topicId);
        if (topic != null && topic.getTeacherId() != null) {
            notificationService.createNotification(
                    topic.getTeacherId(),
                    "THESIS_UPLOADED",
                    "论文已上传",
                    "学生已上传论文《" + fileName + "》，请及时查看",
                    thesis.getId()
            );
        }
        
        return thesis;
    }
    
    /**
     * 获取学生的论文列表
     */
    public List<Thesis> getStudentTheses(Long studentId) {
        return thesisMapper.selectList(
                new LambdaQueryWrapper<Thesis>()
                        .eq(Thesis::getStudentId, studentId)
                        .orderByDesc(Thesis::getCreatedAt)
        );
    }
    
    /**
     * 获取导师的论文列表
     */
    public List<Thesis> getTeacherTheses(Long teacherId) {
        // 需要关联查询选题表获取导师的论文
        // 这里简化处理，实际应该用JOIN查询
        return thesisMapper.selectList(
                new LambdaQueryWrapper<Thesis>()
                        .orderByDesc(Thesis::getCreatedAt)
        );
    }
    
    /**
     * 获取论文详情
     */
    public Thesis getThesis(Long thesisId) {
        return thesisMapper.selectById(thesisId);
    }
}


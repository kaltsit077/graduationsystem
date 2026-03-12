package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.algo.DuplicateCheckAlgorithm;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicTag;
import com.example.graduation.entity.TopicReview;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.TopicReviewMapper;
import com.example.graduation.mapper.TopicTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TopicService {
    
    @Autowired
    private TopicMapper topicMapper;
    
    @Autowired
    private TopicTagMapper topicTagMapper;
    
    @Autowired
    private TopicReviewMapper topicReviewMapper;
    
    @Autowired
    private DuplicateCheckAlgorithm duplicateCheckAlgorithm;
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemSettingService systemSettingService;
    
    /**
     * 创建选题
     */
    @Transactional
    public Topic createTopic(Topic topic, List<String> tags) {
        topic.setStatus(Topic.TopicStatus.DRAFT);
        topic.setCurrentApplicants(0);
        topicMapper.insert(topic);
        
        // 保存标签
        if (tags != null && !tags.isEmpty()) {
            saveTopicTags(topic.getId(), tags);
        }
        
        return topic;
    }
    
    /**
     * 更新选题
     */
    @Transactional
    public Topic updateTopic(Long id, Topic topic, List<String> tags) {
        Topic existing = topicMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("选题不存在");
        }
        
        // 只有草稿或已驳回状态才能编辑
        if (existing.getStatus() != Topic.TopicStatus.DRAFT 
                && existing.getStatus() != Topic.TopicStatus.REJECTED) {
            throw new RuntimeException("当前状态不允许编辑");
        }
        
        topic.setId(id);
        topic.setTeacherId(existing.getTeacherId());
        topicMapper.updateById(topic);
        
        // 更新标签
        if (tags != null) {
            topicTagMapper.delete(new LambdaQueryWrapper<TopicTag>()
                    .eq(TopicTag::getTopicId, id));
            if (!tags.isEmpty()) {
                saveTopicTags(id, tags);
            }
        }
        
        return topic;
    }
    
    /**
     * 保存选题标签
     */
    private void saveTopicTags(Long topicId, List<String> tagNames) {
        for (String tagName : tagNames) {
            if (tagName.length() >= 2 && tagName.length() <= 6) {
                TopicTag tag = new TopicTag();
                tag.setTopicId(topicId);
                tag.setTagName(tagName);
                tag.setWeight(new BigDecimal("0.50"));
                topicTagMapper.insert(tag);
            }
        }
    }
    
    /**
     * 选题去重检测
     */
    public DuplicateCheckResult checkDuplicate(Long topicId, String title, String description) {
        String fullText = (title + " " + (description != null ? description : "")).trim();
        
        // 查询所有历史选题（排除当前选题）
        List<Topic> allTopics = topicMapper.selectList(
                new LambdaQueryWrapper<Topic>()
                        .ne(topicId != null, Topic::getId, topicId)
        );
        
        DuplicateCheckResult result = new DuplicateCheckResult();
        result.setPassed(true);
        result.setMaxSimilarity(0.0);
        
        for (Topic topic : allTopics) {
            String existingText = (topic.getTitle() + " " + 
                    (topic.getDescription() != null ? topic.getDescription() : "")).trim();
            
            double similarity = duplicateCheckAlgorithm.calculateSimilarity(fullText, existingText);
            
            if (similarity > result.getMaxSimilarity()) {
                result.setMaxSimilarity(similarity);
                result.setSimilarTopicId(topic.getId());
                result.setSimilarTopicTitle(topic.getTitle());
            }
        }
        
        result.setPassed(duplicateCheckAlgorithm.isPassed(result.getMaxSimilarity()));
        
        return result;
    }
    
    /**
     * 提交选题审核
     */
    @Transactional
    public void submitForReview(Long topicId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("选题不存在");
        }
        
        if (topic.getStatus() != Topic.TopicStatus.DRAFT 
                && topic.getStatus() != Topic.TopicStatus.REJECTED) {
            throw new RuntimeException("当前状态不允许提交审核");
        }
        
        // 去重检测
        DuplicateCheckResult checkResult = checkDuplicate(topicId, topic.getTitle(), topic.getDescription());
        if (!checkResult.isPassed()) {
            throw new RuntimeException("与历史选题相似度过高（相似度：" + 
                    String.format("%.2f", checkResult.getMaxSimilarity()) + "），请修改后重新提交");
        }
        
        // 更新状态
        topic.setStatus(Topic.TopicStatus.PENDING_REVIEW);
        topicMapper.updateById(topic);
        
        // 发送通知给管理员（这里简化处理，实际应该有管理员列表）
        // notificationService.notifyAdmins(...);
    }
    
    /**
     * 管理员审核选题
     */
    @Transactional
    public void reviewTopic(Long topicId, Long adminId, TopicReview.ReviewResult result, String comment) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("选题不存在");
        }
        
        if (topic.getStatus() != Topic.TopicStatus.PENDING_REVIEW) {
            throw new RuntimeException("选题不在待审核状态");
        }
        
        // 创建审核记录
        TopicReview review = new TopicReview();
        review.setTopicId(topicId);
        review.setAdminId(adminId);
        review.setResult(result);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        topicReviewMapper.insert(review);
        
        // 更新选题状态
        if (result == TopicReview.ReviewResult.PASS) {
            topic.setStatus(Topic.TopicStatus.OPEN);
            // 通知选题开放
            notificationService.createNotification(
                    topic.getTeacherId(),
                    "TOPIC_OPEN",
                    "选题审核通过",
                    "您的选题《" + topic.getTitle() + "》已通过审核，已对学生开放",
                    topicId
            );
        } else {
            topic.setStatus(Topic.TopicStatus.REJECTED);
            // 通知选题驳回
            notificationService.createNotification(
                    topic.getTeacherId(),
                    "TOPIC_REVIEW",
                    "选题审核未通过",
                    "您的选题《" + topic.getTitle() + "》审核未通过：" + comment,
                    topicId
            );
        }
        
        topicMapper.updateById(topic);
    }
    
    /**
     * 获取选题列表
     */
    public List<Topic> getTopics(Topic.TopicStatus status, Long teacherId) {
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Topic::getStatus, status);
        }
        if (teacherId != null) {
            wrapper.eq(Topic::getTeacherId, teacherId);
        }
        wrapper.orderByDesc(Topic::getCreatedAt);
        return topicMapper.selectList(wrapper);
    }
    
    /**
     * 获取已开放的选题列表（学生端）
     */
    public List<Topic> getOpenTopics() {
        // 如果全局选题开关关闭或当前不在开放时间内，直接返回空列表
        if (!systemSettingService.isSelectionOpenNow()) {
            return List.of();
        }
        return getTopics(Topic.TopicStatus.OPEN, null);
    }
    
    /**
     * 去重检测结果
     */
    public static class DuplicateCheckResult {
        private boolean passed;
        private double maxSimilarity;
        private Long similarTopicId;
        private String similarTopicTitle;
        
        // Getters and Setters
        public boolean isPassed() {
            return passed;
        }
        
        public void setPassed(boolean passed) {
            this.passed = passed;
        }
        
        public double getMaxSimilarity() {
            return maxSimilarity;
        }
        
        public void setMaxSimilarity(double maxSimilarity) {
            this.maxSimilarity = maxSimilarity;
        }
        
        public Long getSimilarTopicId() {
            return similarTopicId;
        }
        
        public void setSimilarTopicId(Long similarTopicId) {
            this.similarTopicId = similarTopicId;
        }
        
        public String getSimilarTopicTitle() {
            return similarTopicTitle;
        }
        
        public void setSimilarTopicTitle(String similarTopicTitle) {
            this.similarTopicTitle = similarTopicTitle;
        }
    }
}


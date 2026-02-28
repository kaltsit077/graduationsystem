package com.example.graduation.service;

import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AI选题生成服务（模拟实现）
 * 实际生产环境应该调用大模型API（如OpenAI、智谱等）
 */
@Service
public class AiTopicService {
    
    @Autowired
    private UserTagMapper userTagMapper;
    
    /**
     * 为导师生成候选选题
     */
    public List<CandidateTopic> generateTopicsForTeacher(Long teacherId, int count) {
        // 获取导师标签
        List<UserTag> tags = userTagMapper.selectList(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, teacherId));
        
        // 基于标签生成选题（这里是模拟实现）
        List<CandidateTopic> topics = new ArrayList<>();
        Random random = new Random();
        
        String[] templates = {
                "基于%s的%s系统研究",
                "%s技术在%s领域的应用",
                "%s与%s的融合研究",
                "面向%s的%s方法研究"
        };
        
        for (int i = 0; i < count; i++) {
            String tag1 = tags.isEmpty() ? "智能" : tags.get(random.nextInt(tags.size())).getTagName();
            String tag2 = tags.size() > 1 ? tags.get(random.nextInt(tags.size())).getTagName() : "系统";
            
            String template = templates[random.nextInt(templates.length)];
            String title = String.format(template, tag1, tag2);
            
            CandidateTopic topic = new CandidateTopic();
            topic.setTitle(title);
            topic.setDescription("这是一个基于" + tag1 + "和" + tag2 + "的研究选题，具有重要的学术价值和实践意义。");
            topic.setTags(List.of(tag1, tag2));
            
            topics.add(topic);
        }
        
        return topics;
    }
    
    /**
     * 为学生生成候选选题
     */
    public List<CandidateTopic> generateTopicsForStudent(Long studentId, int count) {
        // 获取学生标签
        List<UserTag> tags = userTagMapper.selectList(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, studentId));
        
        // 类似的生成逻辑
        return generateTopicsForTeacher(studentId, count);
    }
    
    /**
     * 候选选题DTO
     */
    public static class CandidateTopic {
        private String title;
        private String description;
        private List<String> tags;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }
}


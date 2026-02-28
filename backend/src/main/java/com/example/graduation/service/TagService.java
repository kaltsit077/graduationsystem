package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired(required = false)
    private AiTagService aiTagService;
    
    /**
     * 为导师生成标签（基于研究方向）
     * 研究方向标签权重设为0.9
     */
    @Transactional
    public List<UserTag> generateTeacherTags(Long userId, String researchDirection) {
        // 删除现有标签
        userTagMapper.delete(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId));
        
        if (researchDirection == null || researchDirection.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 简单的标签提取逻辑（可以后续优化为更复杂的NLP处理）
        List<UserTag> tags = extractTags(researchDirection, new BigDecimal("0.90"));
        
        // 保存标签
        tags.forEach(tag -> {
            tag.setUserId(userId);
            userTagMapper.insert(tag);
        });
        
        return tags;
    }
    
    /**
     * 为学生生成标签（基于兴趣描述和专业）
     */
    @Transactional
    public List<UserTag> generateStudentTags(Long userId, String interestDesc, String major) {
        // 删除现有标签
        userTagMapper.delete(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId));
        
        List<UserTag> tags = new ArrayList<>();
        
        // 专业相关标签权重0.8
        if (major != null && !major.trim().isEmpty()) {
            tags.addAll(extractTags(major, new BigDecimal("0.80")));
        }
        
        // 兴趣描述标签权重0.9
        if (interestDesc != null && !interestDesc.trim().isEmpty()) {
            tags.addAll(extractTags(interestDesc, new BigDecimal("0.90")));
        }
        
        // 去重并合并权重
        tags = mergeTags(tags);
        
        // 保存标签
        tags.forEach(tag -> {
            tag.setUserId(userId);
            userTagMapper.insert(tag);
        });
        
        return tags;
    }
    
    /**
     * 从文本中提取标签。
     * 若已配置并启用 AI 标签抽取（ai.tag.enabled），优先使用大模型抽取；失败或未配置时使用规则分词。
     */
    private List<UserTag> extractTags(String text, BigDecimal defaultWeight) {
        // 优先使用 AI 抽取（若启用且返回非空）
        if (aiTagService != null) {
            List<String> aiNames = aiTagService.extractTagNames(text);
            if (!aiNames.isEmpty()) {
                List<UserTag> tags = new ArrayList<>();
                for (String name : aiNames) {
                    UserTag tag = new UserTag();
                    tag.setTagName(name);
                    tag.setWeight(defaultWeight);
                    tags.add(tag);
                }
                return tags;
            }
        }

        // 回退：按逗号/顿号/空格分词（与原有逻辑一致）
        List<UserTag> tags = new ArrayList<>();
        String[] keywords = text.split("[,，、\\s]+");
        for (String keyword : keywords) {
            keyword = keyword.trim();
            if (keyword.length() >= 2 && keyword.length() <= 6) {
                UserTag tag = new UserTag();
                tag.setTagName(keyword);
                tag.setWeight(defaultWeight);
                tags.add(tag);
            }
        }
        return tags;
    }
    
    /**
     * 合并重复标签，保留最大权重
     */
    private List<UserTag> mergeTags(List<UserTag> tags) {
        return tags.stream()
                .collect(Collectors.groupingBy(
                        UserTag::getTagName,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    UserTag merged = new UserTag();
                                    merged.setTagName(list.get(0).getTagName());
                                    merged.setWeight(list.stream()
                                            .map(UserTag::getWeight)
                                            .max(BigDecimal::compareTo)
                                            .orElse(BigDecimal.ZERO));
                                    return merged;
                                }
                        )
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户的所有标签
     */
    public List<UserTag> getUserTags(Long userId) {
        return userTagMapper.selectList(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId));
    }
    
    /**
     * 更新用户标签
     */
    @Transactional
    public void updateUserTags(Long userId, List<UserTag> tags) {
        // 删除现有标签
        userTagMapper.delete(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId));
        
        // 插入新标签
        tags.forEach(tag -> {
            tag.setUserId(userId);
            userTagMapper.insert(tag);
        });
    }
}


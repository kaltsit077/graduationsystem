package com.example.graduation.service.impl;

import com.example.graduation.entity.Topic;
import com.example.graduation.entity.UserTag;
import com.example.graduation.entity.TeacherProfile;
import com.example.graduation.mapper.TeacherProfileMapper;
import com.example.graduation.service.EmbeddingService;
import com.example.graduation.service.MatchService;
import com.example.graduation.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 选题匹配服务的基础实现。
 *
 * 说明：
 * - 当前实现严格对应文档中“基于标签重合度 + 标签向量余弦相似度”的基础版本；
 * - 后续接入真正的语义向量（sentence-transformers / bge）时，只需在本类内部替换/扩展实现，
 *   不影响调用方签名。
 */
@Service
public class MatchServiceImpl implements MatchService {

    @Autowired
    private TagService tagService;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    @Override
    public BigDecimal calculateMatchScore(Topic topic, Long studentId) {
        if (topic == null || studentId == null) {
            return new BigDecimal("0.50");
        }

        // 获取学生标签
        List<UserTag> studentTags = tagService.getUserTags(studentId);
        if (studentTags == null || studentTags.isEmpty()) {
            // 没有标签时返回中等匹配度
            return new BigDecimal("0.50");
        }

        StringBuilder sb = new StringBuilder();
        if (topic.getTitle() != null) {
            sb.append(topic.getTitle()).append(' ');
        }
        if (topic.getDescription() != null) {
            sb.append(topic.getDescription());
        }
        String text = sb.toString().toLowerCase();
        if (text.trim().isEmpty()) {
            return new BigDecimal("0.50");
        }

        // 1. 学生标签 ↔ 选题文本：基于标签权重重合度 + 语义相似度
        BigDecimal overlap = calculateOverlapScore(studentTags, text);
        BigDecimal semantic = calculateSemanticSimilarity(topic, studentTags);

        // 2. 学生标签 ↔ 导师画像：基于标签/画像文本的语义相似度
        BigDecimal teacherSimilarity = calculateTeacherStudentSimilarity(topic, studentTags);

        // 3. 融合三个得分（都在 0-1 区间），再统一映射到 0.30-1.00。
        //    当前采用简单线性融合：
        //    final = 0.5 * (0.6 * overlap + 0.4 * semantic) + 0.5 * teacherSimilarity
        BigDecimal alpha = new BigDecimal("0.6");
        BigDecimal oneMinusAlpha = BigDecimal.ONE.subtract(alpha);
        BigDecimal topicSide = overlap.multiply(alpha).add(semantic.multiply(oneMinusAlpha));

        BigDecimal half = new BigDecimal("0.5");
        BigDecimal fused = topicSide.multiply(half).add(teacherSimilarity.multiply(half));

        // 映射到 [0.30, 1.00]
        BigDecimal base = new BigDecimal("0.30");
        BigDecimal scale = new BigDecimal("0.70");
        BigDecimal score = base.add(fused.multiply(scale));

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 标签权重重合度（0-1），对应论文中的“基于标签重合度”的实现。
     */
    private BigDecimal calculateOverlapScore(List<UserTag> tags, String textLowerCase) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal matchedWeight = BigDecimal.ZERO;

        for (UserTag tag : tags) {
            if (tag == null || tag.getWeight() == null || tag.getTagName() == null) {
                continue;
            }
            BigDecimal weight = tag.getWeight();
            totalWeight = totalWeight.add(weight);

            String name = tag.getTagName().toLowerCase();
            if (!name.isEmpty() && textLowerCase.contains(name)) {
                matchedWeight = matchedWeight.add(weight);
            }
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal raw = matchedWeight.divide(totalWeight, 4, RoundingMode.HALF_UP);
        if (raw.compareTo(BigDecimal.ZERO) < 0) {
            raw = BigDecimal.ZERO;
        } else if (raw.compareTo(BigDecimal.ONE) > 0) {
            raw = BigDecimal.ONE;
        }
        return raw;
    }

    /**
     * 语义向量相似度（0-1）：
     * - 若 EmbeddingService 可用，则基于“学生标签文本 + 选题文本”的向量余弦相似度；
     * - 若不可用，则回退到标签余弦相似度（保持兼容性）。
     */
    private BigDecimal calculateSemanticSimilarity(Topic topic, List<UserTag> tags) {
        // 尝试使用 EmbeddingService
        if (embeddingService != null) {
            String studentText = tags.stream()
                    .filter(t -> t != null && t.getTagName() != null)
                    .map(UserTag::getTagName)
                    .collect(Collectors.joining("，"));

            StringBuilder topicSb = new StringBuilder();
            if (topic.getTitle() != null) {
                topicSb.append(topic.getTitle()).append(' ');
            }
            if (topic.getDescription() != null) {
                topicSb.append(topic.getDescription());
            }
            String topicText = topicSb.toString();

            BigDecimal cosine = embedAndCosine(studentText, topicText);
            if (cosine != null) {
                return cosine;
            }
        }

        // 回退：若向量服务不可用，则使用标签余弦相似度
        StringBuilder fallbackText = new StringBuilder();
        if (topic.getTitle() != null) {
            fallbackText.append(topic.getTitle()).append(' ');
        }
        if (topic.getDescription() != null) {
            fallbackText.append(topic.getDescription());
        }
        return calculateTagCosineSimilarity(tags, fallbackText.toString().toLowerCase());
    }

    /**
     * 学生标签 ↔ 导师画像的语义相似度（0-1）：
     * - 若 EmbeddingService 可用，则基于“学生标签文本”和“导师研究方向 + 导师标签”的向量余弦相似度；
     * - 若不可用或教师无画像，则返回 0.5 作为中性值。
     */
    private BigDecimal calculateTeacherStudentSimilarity(Topic topic, List<UserTag> studentTags) {
        if (topic == null || topic.getTeacherId() == null) {
            return new BigDecimal("0.50");
        }
        Long teacherId = topic.getTeacherId();

        List<UserTag> teacherTags = tagService.getUserTags(teacherId);
        TeacherProfile profile = teacherProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getUserId, teacherId)
        );

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
        if (teacherText.isEmpty()) {
            return new BigDecimal("0.50");
        }

        if (embeddingService != null) {
            BigDecimal cosine = embedAndCosine(studentText, teacherText);
            if (cosine != null) {
                return cosine;
            }
        }

        // 若向量服务不可用，则简单回退为 0.5（中性），避免影响整体分布
        return new BigDecimal("0.50");
    }

    /**
     * 通用的“两个文本 → 向量 → 余弦相似度(0-1)”工具方法。
     */
    private BigDecimal embedAndCosine(String textA, String textB) {
        if (embeddingService == null) {
            return null;
        }
        if ((textA == null || textA.isBlank()) || (textB == null || textB.isBlank())) {
            return null;
        }
        float[] vA = embeddingService.embedText(textA);
        float[] vB = embeddingService.embedText(textB);
        if (vA == null || vB == null || vA.length == 0 || vA.length != vB.length) {
            return null;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vA.length; i++) {
            float av = vA[i];
            float bv = vB[i];
            dot += av * bv;
            normA += av * av;
            normB += bv * bv;
        }
        if (dot == 0.0 || normA == 0.0 || normB == 0.0) {
            return null;
        }
        double cosine = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        if (cosine < 0.0) {
            cosine = 0.0;
        } else if (cosine > 1.0) {
            cosine = 1.0;
        }
        return new BigDecimal(cosine).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 标签层面的余弦相似度（原始实现，作为语义向量的回退方案）。
     */
    private BigDecimal calculateTagCosineSimilarity(List<UserTag> tags, String textLowerCase) {
        double dot = 0.0;
        double normStudent = 0.0;
        double normTopic = 0.0;

        for (UserTag tag : tags) {
            if (tag == null || tag.getWeight() == null || tag.getTagName() == null) {
                continue;
            }
            double w = tag.getWeight().doubleValue();
            if (w <= 0) {
                continue;
            }
            normStudent += w * w;

            String name = tag.getTagName().toLowerCase();
            if (!name.isEmpty() && textLowerCase.contains(name)) {
                dot += w * 1.0;
                normTopic += 1.0;
            }
        }

        if (dot == 0.0 || normStudent == 0.0 || normTopic == 0.0) {
            return BigDecimal.ZERO;
        }

        double cosine = dot / (Math.sqrt(normStudent) * Math.sqrt(normTopic));
        if (cosine < 0.0) {
            cosine = 0.0;
        } else if (cosine > 1.0) {
            cosine = 1.0;
        }
        return new BigDecimal(cosine).setScale(4, RoundingMode.HALF_UP);
    }
}


package com.example.graduation.service.impl;

import com.example.graduation.entity.Topic;
import com.example.graduation.entity.UserTag;
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

    @Override
    public BigDecimal calculateMatchScore(Topic topic, Long studentId) {
        if (topic == null || studentId == null) {
            return new BigDecimal("0.50");
        }

        // 获取学生标签
        List<UserTag> tags = tagService.getUserTags(studentId);
        if (tags == null || tags.isEmpty()) {
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

        // 1. 基于标签权重重合度的匹配（0-1）
        BigDecimal overlap = calculateOverlapScore(tags, text);

        // 2. 语义向量相似度（0-1）：优先使用 EmbeddingService，失败时回退到标签余弦
        BigDecimal semantic = calculateSemanticSimilarity(topic, tags);

        // 3. 融合两个得分（都在 0-1 区间），再统一映射到 0.30-1.00。
        //    当前采用简单线性融合：alpha * overlap + (1 - alpha) * semantic
        BigDecimal alpha = new BigDecimal("0.6"); // 权重可后续迁移到配置
        BigDecimal oneMinusAlpha = BigDecimal.ONE.subtract(alpha);
        BigDecimal fused = overlap.multiply(alpha).add(semantic.multiply(oneMinusAlpha));

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

            float[] vStudent = embeddingService.embedText(studentText);
            float[] vTopic = embeddingService.embedText(topicText);
            if (vStudent != null && vTopic != null
                    && vStudent.length == vTopic.length
                    && vStudent.length > 0) {
                double dot = 0.0;
                double normS = 0.0;
                double normT = 0.0;
                for (int i = 0; i < vStudent.length; i++) {
                    float sv = vStudent[i];
                    float tv = vTopic[i];
                    dot += sv * tv;
                    normS += sv * sv;
                    normT += tv * tv;
                }
                if (dot != 0.0 && normS != 0.0 && normT != 0.0) {
                    double cosine = dot / (Math.sqrt(normS) * Math.sqrt(normT));
                    if (cosine < 0.0) {
                        cosine = 0.0;
                    } else if (cosine > 1.0) {
                        cosine = 1.0;
                    }
                    return new BigDecimal(cosine).setScale(4, RoundingMode.HALF_UP);
                }
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


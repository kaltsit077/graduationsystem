package com.example.graduation.service;

import com.example.graduation.entity.Topic;

import java.math.BigDecimal;

/**
 * 统一的选题匹配服务接口。
 *
 * 当前阶段：
 * - 内部仍然基于“标签重合度 + 标签向量余弦”计算得到一个 0-1 之间的 raw 分数，
 * - 再映射为 0.30-1.00 区间的最终匹配度。
 *
 * 后续阶段：
 * - 在不改动本接口签名的前提下，引入真正的语义向量（sentence-transformers / bge 等），
 *   将 {@code score_semantic} 替换为基于文本向量的余弦相似度，实现“标签 + 语义”融合。
 */
public interface MatchService {

    /**
     * 计算学生与选题之间的匹配度。
     *
     * @param topic      选题实体（至少需要 title 和 description）
     * @param studentId  学生 userId
     * @return 映射到 0.30–1.00 区间的匹配度分数
     */
    BigDecimal calculateMatchScore(Topic topic, Long studentId);
}


package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired(required = false)
    private AiTagService aiTagService;

    /**
     * 简单的“专业 → 标准标签”词典，用于将专业名称映射到更加规范的标签集合。
     * 这里给出若干示例，后续可按学校实际专业扩展或改为从数据库/配置加载。
     */
    private static final Map<String, List<String>> MAJOR_TAG_DICT = new HashMap<>();

    /**
     * 回退分词时使用的停用词集合，过滤掉“专业、方向、研究、应用”等低信息量词。
     */
    private static final Set<String> STOP_WORDS = new HashSet<>();

    static {
        // 示例：计算机相关专业
        MAJOR_TAG_DICT.put("计算机", Arrays.asList("计算机科学", "软件工程", "编程基础", "数据结构", "数据库"));
        MAJOR_TAG_DICT.put("软件工程", Arrays.asList("软件工程", "需求分析", "软件设计", "项目管理"));
        MAJOR_TAG_DICT.put("大数据", Arrays.asList("大数据分析", "数据挖掘", "分布式系统"));
        MAJOR_TAG_DICT.put("人工智能", Arrays.asList("人工智能", "机器学习", "深度学习"));

        // 示例：经管类专业
        MAJOR_TAG_DICT.put("金融", Arrays.asList("金融学", "风险管理", "金融市场"));
        MAJOR_TAG_DICT.put("会计", Arrays.asList("会计学", "财务报表分析", "成本管理"));
        MAJOR_TAG_DICT.put("工商管理", Arrays.asList("工商管理", "市场营销", "组织行为"));

        // 示例：教育类专业
        MAJOR_TAG_DICT.put("教育", Arrays.asList("教育技术", "教学设计", "学习分析"));

        // 停用词（可根据需要扩展）
        STOP_WORDS.addAll(Arrays.asList(
                "专业", "方向", "研究", "应用", "分析", "管理",
                "系统", "设计", "开发", "问题", "探讨",
                "的", "和", "与", "及", "相关", "等"
        ));
    }
    
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
        List<UserTag> tags = extractTags(researchDirection, new BigDecimal("0.90"), null, null);
        
        // 保存标签
        tags.forEach(tag -> {
            tag.setUserId(userId);
            userTagMapper.insert(tag);
        });
        
        return tags;
    }
    
    /**
     * 为学生生成标签（基于兴趣描述和专业），可根据 tagMode 控制侧重：
     * - MAJOR：仅基于专业生成
     * - INTEREST：仅基于兴趣描述生成
     * - BOTH（默认）：综合专业与兴趣
     */
    @Transactional
    public List<UserTag> generateStudentTags(Long userId, String interestDesc, String major, String tagMode) {
        // 删除现有标签
        userTagMapper.delete(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, userId));
        
        List<UserTag> tags = new ArrayList<>();

        String mode = (tagMode == null || tagMode.trim().isEmpty())
                ? "BOTH"
                : tagMode.trim().toUpperCase();

        boolean useMajor = "MAJOR".equals(mode) || "BOTH".equals(mode);
        boolean useInterest = "INTEREST".equals(mode) || "BOTH".equals(mode);

        // 1）基于专业的“标准标签 + 文本抽取”联合生成
        if (useMajor && major != null && !major.trim().isEmpty()) {
            String majorText = major.trim();

            // 1.1 专业词典映射：根据专业名称匹配预设标签集合
            String majorLower = majorText.toLowerCase();
            MAJOR_TAG_DICT.forEach((key, value) -> {
                if (majorLower.contains(key.toLowerCase())) {
                    for (String name : value) {
                        UserTag tag = new UserTag();
                        tag.setTagName(name);
                        // 专业映射标签略低于兴趣标签，保留为 0.80
                        tag.setWeight(new BigDecimal("0.80"));
                        tags.add(tag);
                    }
                }
            });

            // 1.2 对专业文本本身再做一次抽取，捕捉词典未覆盖的细分方向
            tags.addAll(extractTags(majorText, new BigDecimal("0.80"), null, null));
        }

        // 2）兴趣描述标签权重 0.90（优先体现学生主观兴趣）
        if (useInterest && interestDesc != null && !interestDesc.trim().isEmpty()) {
            tags.addAll(extractTags(interestDesc.trim(), new BigDecimal("0.90"), null, null));
        }
        
        // 去重并合并权重
        List<UserTag> mergedTags = mergeTags(tags);
        
        // 保存标签
        mergedTags.forEach(tag -> {
            tag.setUserId(userId);
            userTagMapper.insert(tag);
        });
        
        return mergedTags;
    }

    /**
     * 交互式“重抽标签”：保留 pinnedTags，并让新生成的标签不要重复 excludeTagNames（固定的除外）。
     * desiredTotal 为“总标签数（含固定）”目标，默认 5。
     */
    @Transactional
    public List<UserTag> regenerateStudentTags(
            Long userId,
            String interestDesc,
            String major,
            String tagMode,
            List<UserTag> pinnedTags,
            List<String> excludeTagNames,
            Integer desiredTotal) {
        userTagMapper.delete(new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId));

        int total = (desiredTotal == null || desiredTotal <= 0) ? 5 : desiredTotal;
        List<UserTag> pinned = pinnedTags == null ? new ArrayList<>() : pinnedTags.stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTagName() != null && !t.getTagName().trim().isEmpty())
                .map(t -> {
                    UserTag x = new UserTag();
                    x.setTagName(t.getTagName().trim());
                    x.setWeight(t.getWeight());
                    return x;
                })
                .collect(Collectors.toList());

        // 构建排除集合（小写），但固定标签永远允许
        Set<String> pinnedLower = pinned.stream()
                .map(UserTag::getTagName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Set<String> excludeLower = new HashSet<>();
        if (excludeTagNames != null) {
            for (String s : excludeTagNames) {
                if (s == null) continue;
                String t = s.trim();
                if (t.isEmpty()) continue;
                String low = t.toLowerCase();
                if (!pinnedLower.contains(low)) {
                    excludeLower.add(low);
                }
            }
        }

        List<UserTag> generated = new ArrayList<>();
        String mode = (tagMode == null || tagMode.trim().isEmpty())
                ? "BOTH"
                : tagMode.trim().toUpperCase();
        boolean useMajor = "MAJOR".equals(mode) || "BOTH".equals(mode);
        boolean useInterest = "INTEREST".equals(mode) || "BOTH".equals(mode);

        // 只重抽未固定部分：目标数量 = total - pinned
        int remaining = Math.max(0, total - pinned.size());
        if (remaining > 0) {
            String combinedText = buildCombinedText(useInterest ? interestDesc : null, useMajor ? major : null);
            if (combinedText == null || combinedText.isBlank()) {
                throw new IllegalArgumentException("用于生成标签的文本为空，请先填写兴趣描述/专业（或研究方向）");
            }

            BigDecimal weight = "MAJOR".equals(mode) ? new BigDecimal("0.80") : new BigDecimal("0.90");
            generated.addAll(extractTagsAiOnly(combinedText, weight, excludeLower, remaining, 2));
        }

        List<UserTag> all = new ArrayList<>();
        all.addAll(pinned);
        all.addAll(generated);

        List<UserTag> merged = mergeTags(all);
        merged.forEach(tag -> {
            tag.setUserId(userId);
            userTagMapper.insert(tag);
        });
        return merged;
    }
    
    /**
     * 从文本中提取标签。
     * 若已配置并启用 AI 标签抽取（ai.tag.enabled），优先使用大模型抽取；
     * 启用 AI 时不允许回退本地词库/规则分词（避免“看似成功但其实没用上 AI”）。
     */
    private List<UserTag> extractTags(String text, BigDecimal defaultWeight, Set<String> excludeLower, Integer maxCount) {
        // 优先使用 AI 抽取（若启用且返回非空）
        if (aiTagService != null) {
            List<String> aiNames = (excludeLower == null && (maxCount == null || maxCount <= 0))
                    ? aiTagService.extractTagNames(text)
                    : aiTagService.extractTagNames(text, maxCount == null ? 0 : maxCount, excludeLower == null ? null : excludeLower);
            if (aiNames.isEmpty()) {
                throw new RuntimeException("AI 标签生成失败：模型未返回有效标签（请检查 AI 网关、Key、网络与模型名）");
            }
            List<UserTag> tags = new ArrayList<>();
            for (String name : aiNames) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                if (excludeLower != null && excludeLower.contains(name.trim().toLowerCase())) {
                    continue;
                }
                UserTag tag = new UserTag();
                tag.setTagName(name);
                tag.setWeight(defaultWeight);
                tags.add(tag);
            }
            if (maxCount != null && maxCount > 0 && tags.size() > maxCount) {
                return tags.subList(0, maxCount);
            }
            return tags;
        }

        // 回退：按逗号/顿号/空格分词，并结合停用词过滤与频次统计
        Map<String, Integer> freqMap = new HashMap<>();
        String[] keywords = text.split("[,，、\\s]+");
        for (String raw : keywords) {
            String keyword = raw.trim();
            if (keyword.isEmpty()) {
                continue;
            }
            // 统一小写比较，但保存原始形式
            String lower = keyword.toLowerCase();
            // 长度过滤，避免过短/过长的噪声词
            if (lower.length() < 2 || lower.length() > 12) {
                continue;
            }
            // 停用词过滤
            if (STOP_WORDS.contains(lower)) {
                continue;
            }
            if (excludeLower != null && excludeLower.contains(lower)) {
                continue;
            }
            Integer old = freqMap.get(keyword);
            if (old == null) {
                freqMap.put(keyword, 1);
            } else {
                freqMap.put(keyword, old + 1);
            }
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freqMap.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(e -> e.getKey().length()));

        List<UserTag> tags = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (maxCount != null && maxCount > 0 && tags.size() >= maxCount) {
                break;
            }
            String keyword = entry.getKey();
            int freq = entry.getValue();

            UserTag tag = new UserTag();
            tag.setTagName(keyword);

            // 简单的频次加权：出现次数越多，权重略高，在 defaultWeight 基础上线性递增，封顶 1.0
            BigDecimal weight = defaultWeight;
            if (freq > 1) {
                BigDecimal bonus = new BigDecimal("0.05").multiply(new BigDecimal(freq - 1));
                weight = weight.add(bonus);
            }
            if (weight.compareTo(BigDecimal.ONE) > 0) {
                weight = BigDecimal.ONE;
            }
            tag.setWeight(weight);
            tags.add(tag);
        }

        return tags;
    }

    private String buildCombinedText(String interestDesc, String major) {
        String combinedText = "";
        if (interestDesc != null && !interestDesc.trim().isEmpty()) {
            combinedText += interestDesc.trim();
        }
        if (major != null && !major.trim().isEmpty()) {
            if (!combinedText.isEmpty()) {
                combinedText += "；";
            }
            combinedText += major.trim();
        }
        return combinedText;
    }

    /**
     * AI-only 抽取：最多尝试 maxAttempts 次（满足“单次改为 1-2 次”）。
     * 失败将直接抛错，禁止回退本地词库/规则。
     */
    private List<UserTag> extractTagsAiOnly(String text, BigDecimal defaultWeight, Set<String> excludeLower, int maxCount, int maxAttempts) {
        if (aiTagService == null) {
            throw new IllegalStateException("AI 标签服务未启用或未正确配置（ai.tag.enabled/api-url/api-key）");
        }
        int attempts = 0;
        RuntimeException last = null;
        while (attempts < Math.max(1, maxAttempts)) {
            attempts++;
            try {
                List<String> aiNames = aiTagService.extractTagNames(text, maxCount, excludeLower == null ? null : excludeLower);
                if (aiNames == null || aiNames.isEmpty()) {
                    last = new RuntimeException("AI 标签生成失败：模型未返回有效标签");
                    continue;
                }
                List<UserTag> tags = new ArrayList<>();
                for (String name : aiNames) {
                    if (name == null || name.isBlank()) continue;
                    if (excludeLower != null && excludeLower.contains(name.trim().toLowerCase())) continue;
                    UserTag tag = new UserTag();
                    tag.setTagName(name.trim());
                    tag.setWeight(defaultWeight);
                    tags.add(tag);
                }
                if (tags.isEmpty()) {
                    last = new RuntimeException("AI 标签生成失败：返回的标签全部被排除或无效");
                    continue;
                }
                if (tags.size() > maxCount) {
                    return tags.subList(0, maxCount);
                }
                return tags;
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw last == null ? new RuntimeException("AI 标签生成失败") : last;
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


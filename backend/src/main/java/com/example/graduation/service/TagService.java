package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                        // 词典映射标签也使用 0.30–1.00 的稳定随机权重，中心偏向 0.80
                        tag.setWeight(sampleWeight(majorText, name, new BigDecimal("0.80")));
                        tag.setTagType("MAJOR");
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
            String majorCourses,
            String tagMode,
            List<UserTag> pinnedTags,
            List<String> excludeTagNames,
            Integer desiredTotal) {
        userTagMapper.delete(new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId));

        // 统一限制：单个用户的标签总数上限为 9
        int maxTotal = 9;
        int total = (desiredTotal == null || desiredTotal <= 0) ? 5 : desiredTotal;
        if (total > maxTotal) {
            total = maxTotal;
        }
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
            String interestText = useInterest ? interestDesc : null;
            String majorText = useMajor ? buildMajorText(major, majorCourses) : null;

            boolean hasInterest = interestText != null && !interestText.trim().isEmpty();
            boolean hasMajor = majorText != null && !majorText.trim().isEmpty();

            if (!hasInterest && !hasMajor) {
                throw new IllegalArgumentException("用于生成标签的文本为空，请先填写兴趣描述/专业（或研究方向）");
            }

            // 综合模式下，为避免“兴趣描述更长导致生成结果偏兴趣”，改为分别抽取再合并。
            // 不做强制硬比例，但尽量让两侧都有覆盖。
            if ("BOTH".equals(mode) && hasInterest && hasMajor) {
                int interestCount = (remaining + 1) / 2; // 向上取整
                int majorCount = remaining - interestCount;

                if (interestCount > 0) {
                    String it = interestText == null ? "" : interestText.trim();
                    List<UserTag> part = extractTagsAiOnly(it, new BigDecimal("0.90"), excludeLower, interestCount, 2);
                    part.forEach(t -> {
                        if (t != null) t.setTagType("INTEREST");
                    });
                    generated.addAll(part);
                    // 将已生成的标签加入排除集合，避免专业侧重复
                    for (UserTag t : generated) {
                        if (t != null && t.getTagName() != null) {
                            excludeLower.add(t.getTagName().trim().toLowerCase());
                        }
                    }
                }
                if (majorCount > 0) {
                    String mt = majorText == null ? "" : majorText.trim();
                    List<UserTag> part = extractTagsAiOnly(mt, new BigDecimal("0.80"), excludeLower, majorCount, 2);
                    part.forEach(t -> {
                        if (t != null) t.setTagType("MAJOR");
                    });
                    generated.addAll(part);
                }
            } else {
                // 单侧模式：只使用当前模式允许的文本；若两侧都有但不是 BOTH，则按模式选一侧
                String text = hasInterest
                        ? (interestText == null ? "" : interestText.trim())
                        : (majorText == null ? "" : majorText.trim());
                BigDecimal weight = hasInterest ? new BigDecimal("0.90") : new BigDecimal("0.80");
                List<UserTag> part = extractTagsAiOnly(text, weight, excludeLower, remaining, 2);
                String type = hasInterest ? "INTEREST" : "MAJOR";
                part.forEach(t -> {
                    if (t != null) t.setTagType(type);
                });
                generated.addAll(part);
            }
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

    private String buildMajorText(String major, String majorCourses) {
        String m = major == null ? "" : major.trim();
        String c = majorCourses == null ? "" : majorCourses.trim();
        if (m.isEmpty() && c.isEmpty()) return "";
        if (m.isEmpty()) return c;
        if (c.isEmpty()) return m;
        return m + "；已修课程：" + c;
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
                String cleaned = name.trim();
                if (excludeLower != null && excludeLower.contains(cleaned.toLowerCase())) {
                    continue;
                }
                UserTag tag = new UserTag();
                tag.setTagName(cleaned);
                // 与交互式重生成保持一致：使用 0.30–1.00 间的“稳定随机”权重，
                // 并在 defaultWeight 附近轻微偏置
                tag.setWeight(sampleWeight(text, cleaned, defaultWeight));
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
                    tag.setWeight(sampleWeight(text, name.trim(), defaultWeight));
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
     * 为标签生成一个 0.30-1.00 的“稳定随机”权重，并在 defaultWeight 附近轻微偏置。
     * 目的：
     * - 避免固定 0.8/0.9 与真实情况不符；
     * - 同一用户同一输入下可复现（不每次都抖动）；
     * - 仍允许用户在前端手动调整并覆盖。
     */
    private BigDecimal sampleWeight(String sourceText, String tagName, BigDecimal defaultWeight) {
        String s = (sourceText == null ? "" : sourceText.trim()) + "||" + (tagName == null ? "" : tagName.trim());
        int h = s.hashCode();
        // 映射到 [0,1)
        double u = (h & 0x7fffffff) / (double) Integer.MAX_VALUE;
        // 基础随机权重 [0.30, 1.00]
        double base = 0.30 + 0.70 * u;
        // 以 defaultWeight 为中心做轻微偏置（最多 +/-0.10），保持“兴趣略高、专业略低”的直觉但不死板
        double center = defaultWeight == null ? 0.60 : defaultWeight.doubleValue();
        double biased = base * 0.8 + center * 0.2;
        // 若标签本身在原文本中出现，略微上调（更贴合输入）
        if (sourceText != null && tagName != null && !tagName.isBlank()) {
            if (sourceText.toLowerCase().contains(tagName.toLowerCase())) {
                biased = Math.min(1.0, biased + 0.05);
            }
        }
        BigDecimal w = new BigDecimal(biased).setScale(2, RoundingMode.HALF_UP);
        if (w.compareTo(new BigDecimal("0.30")) < 0) w = new BigDecimal("0.30");
        if (w.compareTo(BigDecimal.ONE) > 0) w = BigDecimal.ONE;
        return w;
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
                                    // 以最大权重条目为准（更符合“用户更看重的标签”）
                                    UserTag best = list.stream()
                                            .filter(Objects::nonNull)
                                            .max(Comparator.comparing(t -> t.getWeight() == null ? BigDecimal.ZERO : t.getWeight()))
                                            .orElse(list.get(0));

                                    merged.setWeight(best.getWeight() == null ? BigDecimal.ZERO : best.getWeight());
                                    String type = best.getTagType();
                                    // 兜底：若缺失 type，则按旧逻辑用权重推断
                                    if (type == null || type.trim().isEmpty()) {
                                        BigDecimal w = merged.getWeight() == null ? BigDecimal.ZERO : merged.getWeight();
                                        type = w.compareTo(new BigDecimal("0.85")) >= 0 ? "INTEREST" : "MAJOR";
                                    }
                                    merged.setTagType(type);
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
        if (tags == null) {
            return;
        }
        tags.forEach(tag -> {
            if (tag == null || tag.getTagName() == null || tag.getTagName().trim().isEmpty()) {
                return;
            }
            tag.setUserId(userId);
            tag.setTagName(tag.getTagName().trim());
            // 权重做兜底与裁剪，避免前端传空或越界
            BigDecimal w = tag.getWeight() == null ? new BigDecimal("0.90") : tag.getWeight();
            if (w.compareTo(BigDecimal.ZERO) < 0) w = BigDecimal.ZERO;
            if (w.compareTo(BigDecimal.ONE) > 0) w = BigDecimal.ONE;
            tag.setWeight(w);
            // 类型兜底与裁剪（允许前端自定义专业/兴趣）
            String type = tag.getTagType();
            if (type == null || type.trim().isEmpty()) {
                type = w.compareTo(new BigDecimal("0.85")) >= 0 ? "INTEREST" : "MAJOR";
            } else {
                type = type.trim().toUpperCase();
                if (!"MAJOR".equals(type) && !"INTEREST".equals(type)) {
                    type = w.compareTo(new BigDecimal("0.85")) >= 0 ? "INTEREST" : "MAJOR";
                }
            }
            tag.setTagType(type);
            userTagMapper.insert(tag);
        });
    }
}


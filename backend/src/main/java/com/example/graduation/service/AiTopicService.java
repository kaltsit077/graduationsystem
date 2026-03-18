package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.config.AiTagProperties;
import com.example.graduation.dto.AiGenerateTopicsRequest;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.UserTagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI选题生成服务（模拟实现）
 * 默认优先调用与 AiTagService 相同的大模型接口（如 Gemini Flash），
 * 若未启用或调用失败，则回退到本地模板生成。
 */
@Service
public class AiTopicService {

    private static final Logger log = LoggerFactory.getLogger(AiTopicService.class);
    /**
     * 统一 Markdown 结构的选题生成提示词模板。
     * 占位依次为：导师标签文本、生成数量、选题需求说明。
     * 使用真实换行保证模型看到清晰块结构，便于严格按五维格式输出。
     */
    private static final String TOPIC_PROMPT_TEMPLATE =
            "你是高校本科毕业论文选题系统中的“导师选题生成助手”。现在给你一位导师的标签画像和一段选题需求说明，"
                    + "请在这些约束下，生成若干条适合作为本科四年级学生毕业论文的候选选题，每条都要尽量具体、可落地。\n\n"
                    + "【导师标签】%s\n"
                    + "【生成数量】大约 %d 个\n"
                    + "【选题需求说明】%s\n\n"
                    + "请特别注意：\n"
                    + "1. 即使“选题需求说明”很简略，你也必须根据导师标签和已有信息，合理补全业务场景、能力要求、数据需求和创新点，但不要违背已有约束。\n"
                    + "2. 所有候选选题都必须适合普通本科四年级学生在一个学期左右完成，不要设计过于宏大或依赖大型工程实施的题目。\n"
                    + "3. 选题名称应同时体现【研究方法】和【应用场景】（如“基于整数规划的××库存优化研究”），避免只有宽泛话题。\n\n"
                    + "输出格式要求（必须严格遵守）：\n"
                    + "每个选题必须完整包含以下五个小标题及其具体内容，缺一不可：研究背景、主要内容、能力要求、数据/资源需求、创新点。不要只输出选题名称和一句话描述。\n\n"
                    + "请严格按照下面的 Markdown 模板为每一个选题输出内容，每个选题之间空一行，不要添加序号、解释或其他多余文字，字段名和顺序保持不变：\n\n"
                    + "### 选题名称\n"
                    + "[简洁明了的题目，包含研究方法 + 应用场景]\n\n"
                    + "- 研究背景：[结合行业/业务背景，说明选题意义，约 100 字；如未提供背景说明，则根据导师标签合理假设一个典型场景]\n"
                    + "- 主要内容：[拆解 3–4 个关键研究要点，用短句概括，如“构建 ×× 模型；设计 ×× 算法；搭建 ×× 原型系统”等]\n"
                    + "- 能力要求：[学生需要掌握或愿意学习的主要算法、方法和工具，如“线性/整数规划”“Python + Gurobi”“时间序列预测”等]\n"
                    + "- 数据/资源需求：[完成该题需要的数据和资源，如“近 3 个月门店销售与库存数据”“可访问企业导师进行访谈”等；若未提供，则给出不过分理想化的假设]\n"
                    + "- 创新点：[与传统课程作业或常见论文选题相比的区别，如方法组合创新、场景精细化建模或预测+决策/仿真+可视化的有效结合，避免空泛表述]\n\n"
                    + "请直接给出若干组按照以上模板编写好的候选选题，不要再输出其他解释性文字。";

    @Autowired
    private UserTagMapper userTagMapper;

    @Autowired
    private AiTagProperties properties;

    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 基于请求 DTO 为导师生成候选选题（支持多维度选题需求输入）。
     * 若 DTO 中填写了任选需求字段（背景/要点/能力/数据/创新点），则组装为结构化“选题需求说明”；
     * 否则使用 preferenceHint 作为整体说明。优先调用大模型，失败时回退到本地模板。
     */
    public List<CandidateTopic> generateTopicsForTeacher(Long teacherId, AiGenerateTopicsRequest requestDto) {
        if (requestDto == null) {
            log.debug("AI 选题生成: requestDto 为空，使用默认参数");
            return generateTopicsForTeacher(teacherId, 5, null, null);
        }
        int count = requestDto.getCount() != null
                ? Math.max(1, Math.min(20, requestDto.getCount()))
                : 5;
        List<String> tagNames = requestDto.getTagNames();
        String demandText = buildDemandText(requestDto);
        log.info("AI 选题生成 开始: teacherId={}, count={}, tagCount={}, hasDemand={}",
                teacherId, count, tagNames != null ? tagNames.size() : 0, demandText != null && !demandText.isEmpty());
        return generateTopicsForTeacher(teacherId, count, tagNames, demandText);
    }

    /**
     * 从请求 DTO 组装“选题需求说明”文本：若任一类需求非空则按维度拼接；否则使用 preferenceHint。
     */
    private String buildDemandText(AiGenerateTopicsRequest req) {
        String bg = trim(req.getBackgroundHint());
        String content = trim(req.getContentHint());
        String ability = trim(req.getAbilityHint());
        String data = trim(req.getDataHint());
        String innovation = trim(req.getInnovationHint());
        if (bg.isEmpty() && content.isEmpty() && ability.isEmpty() && data.isEmpty() && innovation.isEmpty()) {
            String pref = trim(req.getPreferenceHint());
            return pref.isEmpty() ? null : pref;
        }
        StringBuilder sb = new StringBuilder();
        if (!bg.isEmpty()) sb.append("【行业/业务背景】").append(bg).append("\n");
        if (!content.isEmpty()) sb.append("【主要研究要点】").append(content).append("\n");
        if (!ability.isEmpty()) sb.append("【能力/工具要求】").append(ability).append("\n");
        if (!data.isEmpty()) sb.append("【数据/资源】").append(data).append("\n");
        if (!innovation.isEmpty()) sb.append("【创新点期望】").append(innovation).append("\n");
        String pref = trim(req.getPreferenceHint());
        if (!pref.isEmpty()) sb.append("【其他说明】").append(pref);
        return sb.toString().trim();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 为导师生成候选选题（可指定用于生成的标签和选题需求说明文本）
     * 优先使用大模型生成；若未启用或失败则使用本地模板生成。
     */
    public List<CandidateTopic> generateTopicsForTeacher(Long teacherId, int count, List<String> preferredTagNames, String preferenceHint) {
        // 获取导师标签
        List<UserTag> allTags = userTagMapper.selectList(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, teacherId));

        if (allTags == null) {
            allTags = new ArrayList<>();
        }

        // 若前端勾选了特定标签，则只在这些标签中采样
        List<UserTag> tags = new ArrayList<>();
        if (preferredTagNames != null && !preferredTagNames.isEmpty()) {
            Set<String> preferLower = new HashSet<>();
            for (String name : preferredTagNames) {
                if (name != null) {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        preferLower.add(trimmed.toLowerCase());
                    }
                }
            }
            for (UserTag t : allTags) {
                if (t.getTagName() == null) continue;
                String lower = t.getTagName().trim().toLowerCase();
                if (preferLower.contains(lower)) {
                    tags.add(t);
                }
            }
        }

        // 如果筛选后为空，则退回到全部标签
        if (tags.isEmpty()) {
            tags = allTags;
        }

        // 若启用了 AI，则优先调用大模型生成题目
        List<CandidateTopic> aiTopics = generateWithAi(tags, count, preferenceHint);
        if (!aiTopics.isEmpty()) {
            log.info("AI 选题生成 使用大模型结果: teacherId={}, 返回数量={}", teacherId, aiTopics.size());
            return aiTopics;
        }

        // 回退：基于标签的本地模板生成（无五维结构，仅标题+简短描述+标签）
        log.info("AI 选题生成 回退到本地模板: teacherId={}, count={}, 原因: 大模型未启用或调用/解析失败", teacherId, count);
        List<CandidateTopic> topics = new ArrayList<>();
        Random random = new Random();

        String[] templates = {
                "基于%s的%s系统研究",
                "%s技术在%s领域的应用",
                "%s与%s的融合研究",
                "面向%s的%s方法研究"
        };

        Set<String> usedTitles = new HashSet<>();
        Set<String> usedPairs = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String tag1 = tags.isEmpty() ? "智能" : tags.get(random.nextInt(tags.size())).getTagName();
            String tag2 = tags.size() > 1 ? tags.get(random.nextInt(tags.size())).getTagName() : "系统";

            // 尽量避免重复的标签组合
            if (!tags.isEmpty()) {
                int attempt = 0;
                while (attempt < 5) {
                    String key = (tag1 + "||" + tag2).toLowerCase();
                    if (!usedPairs.contains(key)) {
                        usedPairs.add(key);
                        break;
                    }
                    tag1 = tags.get(random.nextInt(tags.size())).getTagName();
                    tag2 = tags.size() > 1 ? tags.get(random.nextInt(tags.size())).getTagName() : "系统";
                    attempt++;
                }
            }

            String template = templates[random.nextInt(templates.length)];
            String title = String.format(template, tag1, tag2);

            // 避免标题重复
            if (usedTitles.contains(title)) {
                continue;
            }
            usedTitles.add(title);

            CandidateTopic topic = new CandidateTopic();
            topic.setTitle(title);

            StringBuilder desc = new StringBuilder();
            desc.append("这是一个围绕「").append(tag1).append("」与「").append(tag2).append("」展开的研究选题，");
            desc.append("具有一定的学术价值和实践意义。");
            if (preferenceHint != null && !preferenceHint.trim().isEmpty()) {
                desc.append("本题目偏向：").append(preferenceHint.trim()).append("。");
            }
            topic.setDescription(desc.toString());
            topic.setTags(List.of(tag1, tag2));

            topics.add(topic);
        }

        return topics;
    }

    /**
     * 使用与 AiTagService 相同的大模型接口生成候选选题。
     * 若未启用或解析失败，返回空列表，由调用方回退到本地模板。
     */
    private List<CandidateTopic> generateWithAi(List<UserTag> tags, int count, String preferenceHint) {
        if (tags == null || tags.isEmpty()) {
            log.warn("AI 选题生成: 标签为空，跳过大模型调用");
            return Collections.emptyList();
        }
        if (count <= 0) {
            count = 5;
        }
        if (!properties.isEnabled()) {
            log.info("AI 选题生成: 大模型未启用(ai.enabled=false)，将使用本地模板");
            return Collections.emptyList();
        }

        String tagText = String.join("、", tags.stream().map(UserTag::getTagName).filter(Objects::nonNull).toList());
        String pref = (preferenceHint == null || preferenceHint.trim().isEmpty())
                ? "（未特别说明），请主要根据导师标签和典型教学/实践场景生成。"
                : preferenceHint.trim();

        log.debug("AI 选题生成: 调用大模型, 标签数={}, count={}, 需求长度={}", tags.size(), count, pref.length());
        String prompt = String.format(TOPIC_PROMPT_TEMPLATE, tagText, count, pref);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        // 生成类任务：适当降低随机性与不必要的长输出，减少模型推理耗时
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", Math.max(800, Math.min(2500, count * 450)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body == null) {
                log.warn("AI 选题生成: 大模型响应 body 为空");
                return Collections.emptyList();
            }

            Object choices = body.get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
                log.warn("AI 选题生成: 大模型响应中 choices 为空或非列表, body keys={}", body.keySet());
                return Collections.emptyList();
            }

            Object first = ((List<?>) choices).get(0);
            if (!(first instanceof Map)) {
                return Collections.emptyList();
            }

            Object message = ((Map<?, ?>) first).get("message");
            if (!(message instanceof Map)) {
                return Collections.emptyList();
            }

            Object content = ((Map<?, ?>) message).get("content");
            if (content == null || content.toString().isBlank()) {
                log.warn("AI 选题生成: 大模型返回 content 为空");
                return Collections.emptyList();
            }

            String raw = content.toString();
            List<CandidateTopic> topics = parseAiTopics(raw, count);
            if (!topics.isEmpty()) {
                log.info("AI 选题生成 成功: 解析出 {} 条选题, 原始响应长度={}", topics.size(), raw.length());
            } else {
                log.warn("AI 选题生成 解析失败: 返回内容为空或格式不符合 Markdown 模板, 原始长度={}, 前200字=[{}]",
                        raw.length(), raw.length() > 200 ? raw.substring(0, 200).replace("\n", " ") : raw);
            }
            return topics;
        } catch (Exception e) {
            log.warn("AI 选题生成 请求异常，将使用本地模板生成: {} ", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析模型输出的多行字符串为候选选题列表。
     * 期望输出为若干个按统一 Markdown 模板书写的选题块：
     * (1) "### 选题名称" 后跟一行实际题目，再跟五个 "- 字段名：..." 行；
     * (2) 或 "### 实际题目" 单行标题，再跟五个 "- 字段名：..." 行。
     * 不同选题之间以至少一个空行分隔。
     */
    private List<CandidateTopic> parseAiTopics(String raw, int maxCount) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        // 若被包在 markdown 代码块内，去掉围栏便于按行解析
        String normalized = raw.trim();
        if (normalized.startsWith("```")) {
            int end = normalized.indexOf("```", 3);
            if (end > 0) {
                normalized = normalized.substring(3, end).trim();
            } else {
                normalized = normalized.replaceFirst("^```\\w*\\n?", "");
            }
        }

        String[] lines = normalized.split("\\r?\\n");
        List<CandidateTopic> result = new ArrayList<>();

        String currentTitle = null;
        List<String> currentDescLines = new ArrayList<>();
        boolean nextLineIsTitle = false; // 上一行为 "### 选题名称" 时，下一行才是真实题目

        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();

            if (trimmed.startsWith("### ")) {
                // 先收尾上一条
                if (currentTitle != null && !nextLineIsTitle) {
                    CandidateTopic topic = buildCandidateTopic(currentTitle, currentDescLines);
                    if (topic != null) {
                        result.add(topic);
                        if (maxCount > 0 && result.size() >= maxCount) {
                            return result;
                        }
                    }
                }
                String afterHash = trimmed.replaceFirst("^#+\\s*", "").trim();
                if ("选题名称".equals(afterHash)) {
                    currentTitle = null;
                    currentDescLines = new ArrayList<>();
                    nextLineIsTitle = true;
                } else {
                    currentTitle = afterHash;
                    currentDescLines = new ArrayList<>();
                    nextLineIsTitle = false;
                }
            } else {
                if (nextLineIsTitle && currentTitle == null) {
                    if (!trimmed.isEmpty()) {
                        currentTitle = trimmed;
                        nextLineIsTitle = false;
                    }
                } else if (currentTitle != null) {
                    currentDescLines.add(line);
                }
            }
        }

        if (currentTitle != null && !nextLineIsTitle && (maxCount <= 0 || result.size() < maxCount)) {
            CandidateTopic topic = buildCandidateTopic(currentTitle, currentDescLines);
            if (topic != null) {
                result.add(topic);
            }
        }

        return result;
    }

    /**
     * 将标题和 Markdown 描述行构造成 CandidateTopic。
     * 目前仅提取标题，并将剩余内容整体作为 description，保留 Markdown 以便前端直接展示。
     */
    private CandidateTopic buildCandidateTopic(String title, List<String> descLines) {
        if (title == null) {
            return null;
        }
        String trimmedTitle = title.trim();
        if (trimmedTitle.isEmpty()) {
            return null;
        }
        CandidateTopic topic = new CandidateTopic();
        topic.setTitle(trimmedTitle);
        if (descLines != null && !descLines.isEmpty()) {
            String desc = String.join("\n", descLines).trim();
            topic.setDescription(desc.isEmpty() ? null : desc);
        } else {
            topic.setDescription(null);
        }
        topic.setTags(null);
        return topic;
    }

    /**
     * 兼容旧调用：不指定标签和偏好说明
     */
    public List<CandidateTopic> generateTopicsForTeacher(Long teacherId, int count) {
        return generateTopicsForTeacher(teacherId, count, null, null);
    }
    
    /**
     * 为学生生成候选选题
     */
    public List<CandidateTopic> generateTopicsForStudent(Long studentId, int count) {
        // 目前复用导师侧逻辑：只根据学生标签生成题目
        return generateTopicsForTeacher(studentId, count, null, null);
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


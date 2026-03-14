package com.example.graduation.service;

import com.example.graduation.config.AiTagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class AiTagService {

    private static final Logger log = LoggerFactory.getLogger(AiTagService.class);

    private static final String PROMPT_TEMPLATE =
            "你是高校毕业论文选题匹配系统的标签生成助手。"
                    + "现在给你一段描述（可能是学生的专业和兴趣，也可能是导师的研究方向），"
                    + "请根据语义抽象出 2-6 个【高质量、可用于选题匹配的主题标签】。"
                    + "要求："
                    + "1）标签要与文本内容高度相关，不要只是简单拆分原句；"
                    + "2）每个标签 2-6 个汉字或一个简短英文短语；"
                    + "3）只输出标签本身，用中文逗号分隔；"
                    + "4）不要序号、不要换行、不要解释。"
                    + "\\n\\n文本：%s";

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[,，、;；\\s]+");

    private final AiTagProperties properties;
    private final RestTemplate restTemplate;

    public AiTagService(AiTagProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 从一段文本中抽取标签名（关键词）列表。
     * 若未启用、调用失败或解析结果为空，返回空列表，由调用方回退到规则逻辑。
     */
    public List<String> extractTagNames(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }

        String prompt = String.format(PROMPT_TEMPLATE, text.trim());
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

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

            if (response.getBody() == null) {
                return Collections.emptyList();
            }

            Object choices = response.getBody().get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
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
                return Collections.emptyList();
            }

            List<String> tags = parseTagNames(content.toString());
            if (!tags.isEmpty()) {
                log.info("AI 标签抽取成功，文本长度={}，生成标签={}", text.length(), tags);
            } else {
                log.info("AI 标签抽取返回内容为空或未解析出有效标签，文本长度={}", text.length());
            }
            return tags;
        } catch (Exception e) {
            log.warn("AI 标签抽取请求失败，将使用规则抽取: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 交互式抽取：支持“期望数量”和“排除标签”。
     * - desiredCount: 希望模型输出的标签数量（<=0 则不强制）
     * - excludeTagNames: 模型不应输出的标签（可为空）
     */
    public List<String> extractTagNames(String text, int desiredCount, Collection<String> excludeTagNames) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT_TEMPLATE.formatted(text.trim()));
        if (desiredCount > 0) {
            sb.append("\\n\\n额外要求：请输出 ").append(desiredCount).append(" 个标签。");
        }
        if (excludeTagNames != null) {
            List<String> cleaned = excludeTagNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
            if (!cleaned.isEmpty()) {
                sb.append("\\n额外要求：不要输出以下任何标签（含同义近似表述）：");
                sb.append(String.join("、", cleaned));
                sb.append("。");
            }
        }

        String prompt = sb.toString();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

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

            if (response.getBody() == null) {
                return Collections.emptyList();
            }

            Object choices = response.getBody().get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
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
                return Collections.emptyList();
            }

            List<String> tags = parseTagNames(content.toString());
            if (excludeTagNames != null && !excludeTagNames.isEmpty() && !tags.isEmpty()) {
                Set<String> excludeLower = excludeTagNames.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                tags = tags.stream()
                        .filter(t -> !excludeLower.contains(t.toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (desiredCount > 0 && tags.size() > desiredCount) {
                tags = tags.subList(0, desiredCount);
            }

            if (!tags.isEmpty()) {
                log.info("AI 标签抽取成功(交互)，文本长度={}，生成标签={}", text.length(), tags);
            } else {
                log.info("AI 标签抽取(交互)返回内容为空或未解析出有效标签，文本长度={}", text.length());
            }
            return tags;
        } catch (Exception e) {
            log.warn("AI 标签抽取请求失败(交互)，将使用规则抽取: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析模型返回的字符串为标签名列表：按逗号/顿号/分号/空格切分，过滤长度 2-6。
     */
    private List<String> parseTagNames(String raw) {
        return SPLIT_PATTERN.splitAsStream(raw)
                .map(String::trim)
                .filter(s -> s.length() >= 2 && s.length() <= 6)
                .distinct()
                .collect(Collectors.toList());
    }
}

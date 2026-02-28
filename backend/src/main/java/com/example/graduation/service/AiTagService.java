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

/**
 * 使用大模型从文本中抽取标签（关键词）。
 * 需配置 ai.tag.enabled=true 及 api-url、api-key，兼容 OpenAI 格式的聊天接口。
 */
@Service
public class AiTagService {

    private static final Logger log = LoggerFactory.getLogger(AiTagService.class);

    private static final String PROMPT_TEMPLATE =
            "从以下文本中抽取2-6个关键词作为标签，用于毕业选题方向匹配。"
                    + "要求：仅返回逗号分隔的标签，每个标签2-6个汉字或英文词，不要编号、不要解释。\n\n文本：%s";

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

            return parseTagNames(content.toString());
        } catch (Exception e) {
            log.warn("AI 标签抽取请求失败，将使用规则抽取: {}", e.getMessage());
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

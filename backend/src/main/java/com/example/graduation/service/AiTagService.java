package com.example.graduation.service;

import com.example.graduation.config.AiTagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class AiTagService {

    private static final Logger log = LoggerFactory.getLogger(AiTagService.class);

    private static final String PROMPT_TEMPLATE =
            "你是高校毕业论文选题匹配系统的标签生成助手。"
                    + "现在给你一段描述（可能是学生的课程学习内容或者兴趣爱好，也可能是导师的研究领域和研究方向），"
                    + "请根据语义抽象出 3-6 个【高质量、可用于选题匹配的主题标签或者关键词】。"
                    + "要求："
                    + "1）标签要与文本内容高度相关，不要只是简单拆分原句；"
                    + "2）每个标签 2-6 个汉字或一个简短英文短语，不要输出任何解释、代码块、序号或多余文本；"
                    + "3）只输出标签本身；"
                    + "4）输出必须为 JSON 数组（例如：[\"数据挖掘\",\"供应链优化\"]），不要输出任何解释、代码块、序号或多余文本。"
                    + "\\n\\n文本：%s";

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[,，、;；\\s]+");
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"(.*?)\"");
    private static final Pattern HAS_CJK_PATTERN = Pattern.compile("[\\p{IsHan}]");
    private static final Pattern HAS_DIGIT_PATTERN = Pattern.compile("\\d");

    private static final Set<String> BAD_TAGS_LOWER = new HashSet<>();
    static {
        BAD_TAGS_LOWER.addAll(Arrays.asList(
                "fetch", "request", "requests", "response", "error", "fail", "failed",
                "http", "https", "api", "json",
                "子调用", "调用", "请求", "失败", "错误", "异常", "超时", "重试"
        ));
    }

    private final AiTagProperties properties;
    private final RestTemplate restTemplate;
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    /**
     * 简单内存缓存：同一 prompt 在短时间内重复点击/重试时直接复用结果，显著降低体感延迟与 AI 网关压力。
     */
    private static final long CACHE_TTL_MS = 2 * 60 * 1000L; // 2 分钟
    private static final int CACHE_MAX_SIZE = 800;
    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();

    public AiTagService(AiTagProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 从一段文本中抽取标签名（关键词）列表。
     * 若未启用、调用失败或解析结果为空，返回空列表，由调用方回退到规则逻辑。
     */
    public List<String> extractTagNames(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }

        String prompt = String.format(PROMPT_TEMPLATE, text.trim());
        String apiUrl = Objects.requireNonNull(properties.getApiUrl(), "ai.tag.api-url 不能为空");
        String model = Objects.requireNonNull(properties.getModel(), "ai.tag.model 不能为空");

        // cache hit
        List<String> cached = getCached(model, prompt);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", prompt);
        requestBody.put("messages", Collections.singletonList(msg));
        requestBody.put("temperature", 0.5);
        requestBody.put("max_tokens", 256);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(properties.getApiKey(), "ai.tag.api-key 不能为空"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    MAP_TYPE
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return Collections.emptyList();
            }

            Object choices = body.get("choices");
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
            if (content == null || content.toString().trim().isEmpty()) {
                return Collections.emptyList();
            }

            List<String> tags = parseTagNames(content.toString());
            if (!tags.isEmpty()) {
                log.info("AI 标签抽取成功，文本长度={}，生成标签={}", text.length(), tags);
                putCache(model, prompt, tags);
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
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(PROMPT_TEMPLATE, text.trim()));
        if (desiredCount > 0) {
            sb.append("\\n\\n额外要求：请输出 ").append(desiredCount).append(" 个标签。");
        }
        if (excludeTagNames != null) {
            List<String> cleaned = excludeTagNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (!cleaned.isEmpty()) {
                sb.append("\\n额外要求：不要输出以下任何标签（含同义近似表述）：");
                sb.append(String.join("、", cleaned));
                sb.append("。");
            }
        }

        String prompt = sb.toString();
        String apiUrl = Objects.requireNonNull(properties.getApiUrl(), "ai.tag.api-url 不能为空");
        String model = Objects.requireNonNull(properties.getModel(), "ai.tag.model 不能为空");

        List<String> cached = getCached(model, prompt);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", prompt);
        requestBody.put("messages", Collections.singletonList(msg));
        requestBody.put("temperature", 0.4);
        requestBody.put("max_tokens", 256);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(properties.getApiKey(), "ai.tag.api-key 不能为空"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    MAP_TYPE
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return Collections.emptyList();
            }

            Object choices = body.get("choices");
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
            if (content == null || content.toString().trim().isEmpty()) {
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
                putCache(model, prompt, tags);
            } else {
                log.info("AI 标签抽取(交互)返回内容为空或未解析出有效标签，文本长度={}", text.length());
            }
            return tags;
        } catch (Exception e) {
            log.warn("AI 标签抽取请求失败(交互)，将使用规则抽取: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> getCached(String model, String prompt) {
        String key = (model == null ? "" : model) + "||" + prompt.hashCode();
        CacheItem item = cache.get(key);
        if (item == null) return null;
        if (System.currentTimeMillis() - item.ts > CACHE_TTL_MS) {
            cache.remove(key);
            return null;
        }
        return item.value;
    }

    private void putCache(String model, String prompt, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        // 简单限流：超出上限时随机清一批（避免引入复杂 LRU）
        if (cache.size() > CACHE_MAX_SIZE) {
            int removed = 0;
            for (String k : cache.keySet()) {
                cache.remove(k);
                removed++;
                if (removed >= 100) break;
            }
        }
        String key = (model == null ? "" : model) + "||" + prompt.hashCode();
        cache.put(key, new CacheItem(System.currentTimeMillis(), new ArrayList<>(tags)));
    }

    private static class CacheItem {
        final long ts;
        final List<String> value;

        CacheItem(long ts, List<String> value) {
            this.ts = ts;
            this.value = value;
        }
    }

    /**
     * 解析模型返回的字符串为标签名列表：
     * - 优先解析 JSON 数组中的字符串项；
     * - 兜底按逗号/顿号/分号/空格切分；
     * - 进行清洗与“明显无效标签”过滤，避免把错误信息/调试词写入标签库。
     */
    private List<String> parseTagNames(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String text = raw.trim();

        // 1) 解析 JSON 数组：抓取引号内字符串
        List<String> fromJson = new ArrayList<>();
        if (text.startsWith("[") && text.contains("\"")) {
            java.util.regex.Matcher m = JSON_STRING_PATTERN.matcher(text);
            while (m.find()) {
                String s = normalizeTagName(m.group(1));
                if (isValidTagName(s)) {
                    fromJson.add(s);
                }
            }
        }
        if (!fromJson.isEmpty()) {
            return fromJson.stream().distinct().collect(Collectors.toList());
        }

        // 2) 兜底：按分隔符切分
        return SPLIT_PATTERN.splitAsStream(text)
                .map(this::normalizeTagName)
                .filter(this::isValidTagName)
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeTagName(String s) {
        if (s == null) return "";
        String x = s.trim();
        // 去掉常见包裹符号
        x = x.replaceAll("^[\\[\\]（）(){}【】<>《》“”\"'`]+", "");
        x = x.replaceAll("[\\[\\]（）(){}【】<>《》“”\"'`]+$", "");
        // 压缩中间空白
        x = x.replaceAll("\\s+", " ");
        return x.trim();
    }

    private boolean isValidTagName(String s) {
        if (s == null) return false;
        String x = s.trim();
        if (x.isEmpty()) return false;
        // 长度约束：避免极短/极长噪声
        if (x.length() < 2 || x.length() > 12) return false;
        // 过滤含数字的“序号/版本号/学号”等
        if (HAS_DIGIT_PATTERN.matcher(x).find()) return false;

        String lower = x.toLowerCase(Locale.ROOT);
        if (BAD_TAGS_LOWER.contains(lower)) return false;
        // 明显的调试/错误语义
        if (lower.contains("error") || lower.contains("fail") || lower.contains("request") || lower.contains("fetch")) return false;
        if (x.contains("失败") || x.contains("错误") || x.contains("异常") || x.contains("子调用") || x.contains("请求")) return false;

        // 允许中文为主；纯英文也可，但要避免被当成请求/日志词
        boolean hasCjk = HAS_CJK_PATTERN.matcher(x).find();
        if (!hasCjk) {
            // 纯英文/缩写：只允许字母和空格/短横线
            if (!lower.matches("[a-z\\- ]{2,12}")) return false;
        }
        return true;
    }
}

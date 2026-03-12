package com.example.graduation.service.impl;

import com.example.graduation.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 基于 HTTP 的 EmbeddingService 实现。
 *
 * 约定本地有一个兼容的向量服务，例如基于 FastAPI + sentence-transformers / bge，
 * 暴露 POST /embed 接口：
 *
 * - 请求体: { "text": "..." }
 * - 响应体: { "vector": [0.1, 0.2, ...] }
 *
 * 配置项（application.yml）示例：
 *
 * embedding:
 *   enabled: true
 *   base-url: http://localhost:8000
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

    private final RestTemplate restTemplate;

    @Value("${embedding.enabled:true}")
    private boolean enabled;

    @Value("${embedding.base-url:http://localhost:8000}")
    private String baseUrl;

    public EmbeddingServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public float[] embedText(String text) {
        if (!enabled) {
            return null;
        }
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "embed" : baseUrl + "/embed";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of("text", text.trim());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> respBody = restTemplate.postForObject(
                    url,
                    entity,
                    Map.class
            );
            if (respBody == null) {
                return null;
            }

            Object vecObj = respBody.get("vector");
            if (!(vecObj instanceof List)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) vecObj;
            if (list.isEmpty()) {
                return null;
            }

            float[] result = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object v = list.get(i);
                if (v == null) {
                    result[i] = 0.0f;
                } else if (v instanceof Number) {
                    result[i] = ((Number) v).floatValue();
                } else {
                    result[i] = Float.parseFloat(v.toString());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("调用 Embedding 服务失败，将回退到标签匹配: {}", e.getMessage());
            return null;
        }
    }
}


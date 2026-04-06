package com.example.graduation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final int DEFAULT_READ_TIMEOUT_MS = 15000;
    private static final int DEFAULT_EMBEDDING_READ_TIMEOUT_MS = 8000;

    /**
     * AI/Embedding 这类外部 HTTP 调用的读取超时。
     * 优先使用 ai.tag.timeout-ms；未配置则回退到默认值。
     */
    @Value("${ai.tag.timeout-ms:" + DEFAULT_READ_TIMEOUT_MS + "}")
    private int readTimeoutMs;

    /**
     * Embedding 服务调用超时（毫秒）。
     * 单次 embed 不应阻塞太久，否则会拖慢学生端“选题中心/匹配度”列表加载。
     */
    @Value("${embedding.timeout-ms:" + DEFAULT_EMBEDDING_READ_TIMEOUT_MS + "}")
    private int embeddingReadTimeoutMs;

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(Math.max(1000, readTimeoutMs));
        return new RestTemplate(factory);
    }

    @Bean(name = "embeddingRestTemplate")
    public RestTemplate embeddingRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(Math.max(1000, embeddingReadTimeoutMs));
        return new RestTemplate(factory);
    }
}

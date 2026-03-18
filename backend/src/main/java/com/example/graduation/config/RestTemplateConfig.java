package com.example.graduation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final int DEFAULT_READ_TIMEOUT_MS = 15000;

    /**
     * AI/Embedding 这类外部 HTTP 调用的读取超时。
     * 优先使用 ai.tag.timeout-ms；未配置则回退到默认值。
     */
    @Value("${ai.tag.timeout-ms:" + DEFAULT_READ_TIMEOUT_MS + "}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(Math.max(1000, readTimeoutMs));
        return new RestTemplate(factory);
    }
}

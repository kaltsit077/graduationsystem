package com.example.graduation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 标签抽取配置。
 * 对应 application.yml 中的 ai.tag.*
 */
@Component
@ConfigurationProperties(prefix = "ai.tag")
public class AiTagProperties {

    /** 是否启用 AI 抽取（未配置或 false 时使用原有 split 逻辑） */
    private boolean enabled = false;

    /** 大模型聊天接口 URL（兼容 OpenAI 格式） */
    private String apiUrl = "https://api.openai.com/v1/chat/completions";

    /** API Key（建议通过环境变量 AI_TAG_API_KEY 设置） */
    private String apiKey = "";

    /** 模型名 */
    private String model = "gpt-3.5-turbo";

    /** 请求超时（毫秒） */
    private int timeoutMs = 10000;

    public boolean isEnabled() {
        return enabled && apiUrl != null && !apiUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}

package com.example.graduation.service;

/**
 * 文本语义向量编码服务接口。
 *
 * 后续通过本接口对接本地部署的 sentence-transformers / bge 模型，
 * 由 MatchService/其它业务按需调用。
 */
public interface EmbeddingService {

    /**
     * 对单段文本进行编码，返回语义向量。
     *
     * @param text 文本内容（允许为任意语言，推荐事先 trim）
     * @return 浮点向量；调用失败或未启用时可返回 null，由上层决定降级策略
     */
    float[] embedText(String text);
}


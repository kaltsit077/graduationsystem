package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGenerateTopicsResultResponse {

    /** 通过去重后的候选题（已按期望数量截断） */
    private List<AiGeneratedTopicResponse> topics;

    /** AI 预生成总数（过滤前） */
    private int generatedCount;

    /** 去重后通过阈值的总数（截断前） */
    private int passedCount;

    /** 因去重或阈值被淘汰的数量 */
    private int eliminatedCount;

    /** 实际返回给前端的数量（通常 <= desiredCount） */
    private int returnedCount;

    /** 前端期望返回数量 */
    private int desiredCount;
}


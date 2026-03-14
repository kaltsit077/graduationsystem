package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGeneratedTopicResponse {

    /** 生成的题目名称 */
    private String title;

    /** 生成的题目描述/背景 */
    private String description;

    /** 由生成服务给出的标签（通常来自导师标签组合） */
    private List<String> tags;

    /** 与历史题库的最大相似度（0-1），便于前端展示给导师参考 */
    private Double maxSimilarity;

    /** 若存在最相似题目，则返回其标题，方便导师了解“撞题对象” */
    private String similarTopicTitle;

    /** 是否通过系统的自动去重阈值检查（只返回通过的候选项） */
    private boolean passed;
}


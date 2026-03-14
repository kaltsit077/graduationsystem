package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class TagRegenerateRequest {
    private String interestDesc;
    private String major;
    /**
     * 标签生成模式：MAJOR / INTEREST / BOTH
     */
    private String tagMode;

    /**
     * 用户想要保留（固定）的标签（不参与重抽）
     */
    private List<UserTagResponse> pinnedTags;

    /**
     * 本轮“黑名单”：重抽时不允许再次生成这些标签（固定的除外）
     */
    private List<String> excludeTagNames;

    /**
     * 期望总标签数（含固定标签），默认 5
     */
    private Integer desiredTotal;
}


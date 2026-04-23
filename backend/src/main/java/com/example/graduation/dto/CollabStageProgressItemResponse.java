package com.example.graduation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollabStageProgressItemResponse {
    private int orderIndex;
    private int phaseIndex;
    private String phaseLabel;
    private String stage;
    private String stageLabel;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    /** NOT_CONFIGURED | NOT_OPEN_YET | OPEN | ENDED */
    private String accessState;
    /** 时间计划列展示文案 */
    private String timePlanText;
    /** NONE | UNDER_REVIEW | NEED_REVISION | APPROVED — 相对「当前最新提交」 */
    private String submissionStatus;
    private String submissionStatusLabel;
    private Long latestThesisId;
    private String latestFileName;
    private LocalDateTime latestUpdatedAt;
}

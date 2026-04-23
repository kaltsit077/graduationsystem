package com.example.graduation.entity;

import java.util.Arrays;
import java.util.List;

/**
 * 协作中心毕设环节（不含「选题」，协作页仅已绑定选题后可见）。
 * 顺序与格子达常见流程对齐，用于进度表展示与目录分栏。
 */
public enum CollabStage {
    TASK_BOOK("任务书"),
    OPENING_REPORT("开题报告"),
    LITERATURE_REVIEW("文献综述"),
    MID_TERM("中期检查"),
    GUIDANCE_RECORD("指导记录表-学生填写"),
    PRE_DEFENSE_THESIS("答辩前论文"),
    DEFENSE_APPLICATION("答辩申请及资格审查"),
    THESIS_REVISION("毕业论文修改说明"),
    BLIND_REVIEW_BEFORE_DEFENSE("答辩前盲评"),
    THESIS_DEFENSE("论文答辩"),
    FINAL_AFTER_DEFENSE("答辩后终稿");

    private final String label;

    CollabStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static List<CollabStage> ordered() {
        return Arrays.asList(values());
    }

    /** 以「中期检查/中期答辩」和「论文答辩」为节点，划分 3 个阶段（阶段内可并行） */
    public enum Phase {
        BEFORE_MIDTERM(1, "第一阶段（中期前）"),
        BEFORE_DEFENSE(2, "第二阶段（答辩前）"),
        AFTER_DEFENSE(3, "第三阶段（答辩后）");

        private final int index;
        private final String label;

        Phase(int index, String label) {
            this.index = index;
            this.label = label;
        }

        public int getIndex() {
            return index;
        }

        public String getLabel() {
            return label;
        }
    }

    public Phase getPhase() {
        switch (this) {
            case TASK_BOOK:
            case OPENING_REPORT:
            case LITERATURE_REVIEW:
            case MID_TERM:
                return Phase.BEFORE_MIDTERM;
            case GUIDANCE_RECORD:
            case PRE_DEFENSE_THESIS:
            case DEFENSE_APPLICATION:
            case THESIS_REVISION:
            case BLIND_REVIEW_BEFORE_DEFENSE:
            case THESIS_DEFENSE:
                return Phase.BEFORE_DEFENSE;
            case FINAL_AFTER_DEFENSE:
            default:
                return Phase.AFTER_DEFENSE;
        }
    }

    public static CollabStage fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("环节不能为空");
        }
        try {
            return CollabStage.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效环节: " + code);
        }
    }
}

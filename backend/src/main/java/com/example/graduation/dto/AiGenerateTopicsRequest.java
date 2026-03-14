package com.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGenerateTopicsRequest {

    /**
     * 期望生成的候选选题数量，默认 5，最大 20。
     */
    private Integer count;

    /**
     * 可选：导师对题目难度/层次的简要描述或自由补充（如“大四本科毕业论文”“偏实践”等）。
     * 当未填写下方五类选题需求时，本字段作为整体“选题需求说明”传入模型。
     */
    private String preferenceHint;

    /**
     * 可选：行业/业务背景说明。如“在连锁奶茶店日常运营中，库存积压与缺货并存……”
     */
    private String backgroundHint;

    /**
     * 可选：希望覆盖的主要研究要点。如“① 日销量预测；② 补货决策模型；③ 可视化看板……”
     */
    private String contentHint;

    /**
     * 可选：对学生能力/工具的要求。如“线性/整数规划，Python+Gurobi……”
     */
    private String abilityHint;

    /**
     * 可选：可提供的数据/资源。如“近 3 个月门店销售与库存数据……”
     */
    private String dataHint;

    /**
     * 可选：期望的创新点或与传统做法的区别。如“结合时序预测的动态补货……”
     */
    private String innovationHint;

    /**
     * 可选：前端勾选用于生成的标签名称列表（来自导师画像）。
     * 若为空则使用导师的全部标签。
     */
    private List<String> tagNames;
}


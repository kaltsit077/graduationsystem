package com.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("thesis_evaluation")
public class ThesisEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long thesisId;

    private Long studentId;

    private Long teacherId;

    private BigDecimal score;

    private BigDecimal defenseScore;

    private BigDecimal reviewScore;

    private String gradeLevel;

    private String comment;

    private BigDecimal studentScore;

    private String studentComment;

    private LocalDateTime createdAt;
}


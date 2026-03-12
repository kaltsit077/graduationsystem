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
@TableName("topic_metrics")
public class TopicMetrics {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long topicId;

    private Long teacherId;

    private Integer totalStudents;

    private BigDecimal avgScore;

    private BigDecimal excellentRatio;

    private BigDecimal failRatio;

    private LocalDateTime lastUpdatedAt;
}


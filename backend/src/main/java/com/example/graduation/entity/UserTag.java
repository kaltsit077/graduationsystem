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
@TableName("user_tag")
public class UserTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String tagName;

    /**
     * 标签类型：MAJOR / INTEREST
     * - MAJOR：专业/基础背景类标签
     * - INTEREST：兴趣/方向偏好类标签
     */
    private String tagType;
    
    private BigDecimal weight; // 0-1之间，研究方向标签权重0.9
    
    private LocalDateTime createdAt;
}


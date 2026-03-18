package com.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String passwordHash;
    
    private String realName;

    /**
     * 用户自定义背景图（静态资源 URL），如：/uploads/backgrounds/xxx.jpg
     */
    private String backgroundUrl;

    /** 背景缩放百分比（50-200） */
    private Integer backgroundScale;
    /** 背景水平位置百分比（0-100） */
    private Integer backgroundPosX;
    /** 背景垂直位置百分比（0-100） */
    private Integer backgroundPosY;
    /** 背景遮罩透明度（0-1，越大越“白”） */
    private Double bgOverlayAlpha;
    /** 内容容器白底透明度（0-1） */
    private Double contentAlpha;
    /** 内容容器毛玻璃强度（px） */
    private Integer contentBlur;
    
    private Role role;
    
    private Integer status; // 0-禁用，1-启用
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum Role {
        STUDENT, TEACHER, ADMIN
    }
}


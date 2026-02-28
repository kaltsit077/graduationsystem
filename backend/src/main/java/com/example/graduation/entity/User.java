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
    
    private Role role;
    
    private Integer status; // 0-禁用，1-启用
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum Role {
        STUDENT, TEACHER, ADMIN
    }
}


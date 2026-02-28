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
@TableName("tag")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private TagType type;
    
    private LocalDateTime createdAt;
    
    public enum TagType {
        TEACHER, STUDENT, TOPIC
    }
}

